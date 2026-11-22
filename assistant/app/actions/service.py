import hashlib
import json
import uuid
from collections.abc import Awaitable, Callable
from datetime import datetime, timezone
from typing import Any

from pydantic import ValidationError
from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.actions.entities import ActionAudit, ActionRevision, PendingAction
from app.actions.types import TERMINAL, ActionStatus, ActionTypeRegistry, ApproverRule, action_types
from app.agent.registry import Capability
from app.core.metrics import ACTIONS
from app.core.problem import ProblemDetailException
from app.governance.service import GovernanceService
from app.messaging.outbox import ACTION_DECIDED, add_event
from app.security.auth import RequestUser
from app.sessions.entities import AgentSession


# (action_id, approver_token) -> final action; DBOS in production, a direct call in tests.
ExecutionRunner = Callable[[str, str], Awaitable[None]]


def now() -> datetime:
    return datetime.now(timezone.utc)


def payload_hash(payload: dict) -> str:
    return hashlib.sha256(json.dumps(payload, sort_keys=True, ensure_ascii=False).encode()).hexdigest()


def _aware(value: datetime) -> datetime:
    # SQLite hands back naive datetimes; Postgres keeps the zone.
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


class ActionService:
    def __init__(self, db: AsyncSession, runner: ExecutionRunner | None = None, types: ActionTypeRegistry = action_types):
        self.db = db
        self.runner = runner
        self.types = types

    async def propose(
        self,
        *,
        type_id: str,
        payload: dict[str, Any],
        actor: RequestUser,
        persona: str,
        capability_id: str,
        allowed_types: frozenset[str],
        session_id: str | None,
        author: str = "ASSISTANT",
        commit: bool = True,
    ) -> PendingAction:
        """With commit False the caller commits, so the proposal lands together with its own writes or not at all."""
        if type_id not in allowed_types:
            raise ValueError(f"Capability {capability_id} may not propose {type_id}")
        action_type = self.types.get(type_id)
        try:
            model = action_type.payload_model.model_validate(payload)
        except ValidationError as exc:
            raise ValueError(f"Invalid {type_id} payload: {exc.errors(include_url=False)}") from exc
        action_id = str(uuid.uuid4())
        clean = model.model_dump(mode="json")
        action = PendingAction(
            id=action_id,
            type=type_id,
            version=1,
            status=ActionStatus.PENDING.value,
            actor_user_id=actor.user_id,
            persona=persona,
            capability_id=capability_id,
            session_id=session_id,
            target_service=action_type.target_service,
            payload=clean,
            preview=action_type.preview(model),
            approver_rule=action_type.approver_rule.value,
            approver_permission=action_type.approver_permission,
            requires_mfa=action_type.requires_mfa,
            idempotency_key=f"assistant-action:{action_id}:v1",
            expires_at=now() + action_type.ttl,
            created_at=now(),
        )
        if action_type.supersede_group and session_id:
            await self._supersede(action_type.supersede_group, session_id, actor.user_id)
        self.db.add(action)
        # The audit row references this one, and the mappers declare no relationship to order the inserts.
        await self.db.flush()
        self.audit(action, "PROPOSED", actor.user_id)
        self._revision(action, author)
        if commit:
            await self.db.commit()
        ACTIONS.labels(type_id, "proposed").inc()
        return action

    async def draft(self, *, capability: Capability, type_id: str, payload: dict[str, Any], actor: RequestUser,
                    session_id: str | None) -> PendingAction:
        """The learner starts a draft by hand (a template, their own lessons, an existing path).

        It lives in an assistant session like a proposal does, so a later turn there sees it and can change it.
        """
        if capability.id in await GovernanceService(self.db).disabled_ids():
            raise ProblemDetailException(403, "CAPABILITY_DISABLED", "Learning path drafts are switched off")
        if session_id:
            session = await self.db.get(AgentSession, session_id)
            if session is None or session.user_id != actor.user_id:
                raise ProblemDetailException(404, "RESOURCE_NOT_FOUND", "Session not found")
            session.updated_at = now()
        else:
            title = payload.get("title") if isinstance(payload.get("title"), str) else None
            session = AgentSession(id=str(uuid.uuid4()), user_id=actor.user_id, persona=capability.persona.value,
                                   placement="learning_path", title=title[:200] if title else None,
                                   last_capability_id=capability.id, created_at=now(), updated_at=now())
            self.db.add(session)
        try:
            return await self.propose(type_id=type_id, payload=payload, actor=actor, persona=capability.persona.value,
                                      capability_id=capability.id, allowed_types=capability.action_types,
                                      session_id=session.id, author="LEARNER")
        except ValueError as exc:
            raise ProblemDetailException(400, "VALIDATION_FAILED", str(exc)) from exc

    async def origins(self, action_ids: list[str]) -> dict[str, str]:
        """Who wrote each draft's first version: ASSISTANT or LEARNER."""
        if not action_ids:
            return {}
        rows = await self.db.execute(select(ActionRevision.pending_action_id, ActionRevision.author).where(
            ActionRevision.pending_action_id.in_(action_ids), ActionRevision.version == 1))
        return {action_id: author for action_id, author in rows.all()}

    async def revisions(self, action_id: str, user: RequestUser) -> list[ActionRevision]:
        """Every version of the draft, oldest first; visible to the same people as the action."""
        await self.get_visible(action_id, user)
        rows = await self.db.scalars(
            select(ActionRevision).where(ActionRevision.pending_action_id == action_id).order_by(ActionRevision.version)
        )
        return list(rows.all())

    async def list_visible(self, user: RequestUser, status: ActionStatus | None, limit: int,
                           session_id: str | None = None) -> list[PendingAction]:
        """The caller's own proposals, plus those waiting on a permission the caller holds."""
        visible = or_(
            PendingAction.actor_user_id == user.user_id,
            PendingAction.approver_permission.in_(sorted(user.permissions) or [""]),
        )
        query = select(PendingAction).where(visible).order_by(PendingAction.created_at.desc()).limit(limit)
        if status:
            query = query.where(PendingAction.status == status.value)
        if session_id:
            query = query.where(PendingAction.session_id == session_id)
        return list((await self.db.scalars(query)).all())

    async def revise(self, action_id: str, user: RequestUser, version: int, payload: dict[str, Any]) -> PendingAction:
        """The learner edits a draft by hand: same action, next version, and a key learning dedupes per version."""
        action = (
            await self.db.scalars(select(PendingAction).where(PendingAction.id == action_id).with_for_update())
        ).one_or_none()
        # Only the person it was proposed for edits it; a reviewer approves or rejects, never rewrites.
        if action is None or action.actor_user_id != user.user_id:
            raise ProblemDetailException(404, "RESOURCE_NOT_FOUND", "Pending action not found")
        if action.status != ActionStatus.PENDING.value:
            raise ProblemDetailException(409, "ACTION_NOT_PENDING", f"The action is already {action.status}")
        if _aware(action.expires_at) <= now():
            self._expire(action)
            await self.db.commit()
            raise ProblemDetailException(409, "ACTION_EXPIRED", "The action expired before it was changed")
        await self._expire_if_disabled(action)
        if action.version != version:
            raise ProblemDetailException(409, "CONFLICT", "The draft changed since it was loaded; reload it")
        action_type = self.types.get(action.type)
        try:
            model = action_type.payload_model.model_validate(payload)
        except ValidationError as exc:
            raise ProblemDetailException(400, "VALIDATION_FAILED", f"Invalid {action.type}: {exc.errors(include_url=False)}") from exc
        action.payload = model.model_dump(mode="json")
        action.preview = action_type.preview(model)
        action.version += 1
        action.idempotency_key = f"assistant-action:{action.id}:v{action.version}"
        self.audit(action, "REVISED", user.user_id)
        self._revision(action, "LEARNER")
        await self.db.commit()
        ACTIONS.labels(action.type, "revised").inc()
        return action

    async def update_draft(self, action_id: str, actor: RequestUser, payload: dict[str, Any]) -> PendingAction:
        """The agent rewrites its own pending draft (a path one phase further): same action, next version.

        Raises ValueError, for the model, when the draft is not this user's or no longer pending.
        """
        action = (
            await self.db.scalars(select(PendingAction).where(PendingAction.id == action_id).with_for_update())
        ).one_or_none()
        if action is None or action.actor_user_id != actor.user_id:
            raise ValueError("That draft does not exist")
        if action.status != ActionStatus.PENDING.value:
            raise ValueError(f"That draft is already {action.status}")
        action_type = self.types.get(action.type)
        try:
            model = action_type.payload_model.model_validate(payload)
        except ValidationError as exc:
            raise ValueError(f"Invalid {action.type} payload: {exc.errors(include_url=False)}") from exc
        action.payload = model.model_dump(mode="json")
        action.preview = action_type.preview(model)
        action.version += 1
        action.idempotency_key = f"assistant-action:{action.id}:v{action.version}"
        self.audit(action, "REVISED", actor.user_id)
        self._revision(action, "ASSISTANT")
        await self.db.commit()
        ACTIONS.labels(action.type, "revised").inc()
        return action

    async def get_visible(self, action_id: str, user: RequestUser) -> PendingAction:
        action = await self.db.get(PendingAction, action_id)
        if action is None or not self._can_see(action, user):
            raise ProblemDetailException(404, "RESOURCE_NOT_FOUND", "Pending action not found")
        return action

    async def approve(self, action_id: str, approver: RequestUser, version: int) -> PendingAction:
        action = await self._decidable(action_id, approver, version)
        ready = self.types.get(action.type).ready
        unfinished = ready(action.payload or {}) if ready else None
        if unfinished:
            raise ProblemDetailException(409, "DRAFT_INCOMPLETE", unfinished)
        action.status = ActionStatus.APPROVED.value
        action.decided_at = now()
        action.decided_by = approver.user_id
        self.audit(action, "APPROVED", approver.user_id)
        await self.db.commit()
        ACTIONS.labels(action.type, "approved").inc()
        if self.runner is None:
            raise RuntimeError("ActionService needs an execution runner to approve")
        # The approver's token authorises the write; it is held in memory only.
        await self.runner(action.id, approver.token)
        await self.db.refresh(action)
        return action

    async def reject(self, action_id: str, approver: RequestUser, version: int, reason: str | None) -> PendingAction:
        action = await self._decidable(action_id, approver, version)
        action.status = ActionStatus.REJECTED.value
        action.decided_at = now()
        action.decided_by = approver.user_id
        action.decision_reason = reason
        self.audit(action, "REJECTED", approver.user_id, detail=reason)
        self.announce(action)
        await self.db.commit()
        ACTIONS.labels(action.type, "rejected").inc()
        return action

    async def expire_due(self, limit: int = 200) -> int:
        due = (
            await self.db.scalars(
                select(PendingAction)
                .where(PendingAction.status == ActionStatus.PENDING.value, PendingAction.expires_at <= now())
                .limit(limit)
            )
        ).all()
        for action in due:
            self._expire(action)
        await self.db.commit()
        return len(due)

    async def expire_for_capability(self, capability_id: str) -> int:
        """A capability switched off leaves none of its actions to be carried out."""
        pending = (
            await self.db.scalars(
                select(PendingAction)
                .where(PendingAction.status == ActionStatus.PENDING.value, PendingAction.capability_id == capability_id)
                .with_for_update()
            )
        ).all()
        for action in pending:
            self._expire(action)
        await self.db.commit()
        return len(pending)

    async def _decidable(self, action_id: str, approver: RequestUser, version: int) -> PendingAction:
        action = (
            await self.db.scalars(select(PendingAction).where(PendingAction.id == action_id).with_for_update())
        ).one_or_none()
        if action is None or not self._can_see(action, approver):
            raise ProblemDetailException(404, "RESOURCE_NOT_FOUND", "Pending action not found")
        if action.status != ActionStatus.PENDING.value:
            raise ProblemDetailException(409, "ACTION_NOT_PENDING", f"The action is already {action.status}")
        if _aware(action.expires_at) <= now():
            self._expire(action)
            await self.db.commit()
            raise ProblemDetailException(409, "ACTION_EXPIRED", "The action expired before it was decided")
        await self._expire_if_disabled(action)
        if action.version != version:
            raise ProblemDetailException(409, "CONFLICT", "The action changed since it was shown; review it again")
        if not self._may_decide(action, approver):
            raise ProblemDetailException(403, "APPROVER_NOT_ALLOWED", "This account may not decide this action")
        if action.requires_mfa and not approver.mfa:
            raise ProblemDetailException(403, "MFA_STEP_UP_REQUIRED", "Confirm with multi-factor authentication first")
        return action

    async def _supersede(self, group: str, session_id: str, actor_user_id: str) -> None:
        group_types = [t.id for t in self.types.all() if t.supersede_group == group]
        earlier = (await self.db.scalars(
            select(PendingAction).where(
                PendingAction.session_id == session_id,
                PendingAction.actor_user_id == actor_user_id,
                PendingAction.status == ActionStatus.PENDING.value,
                PendingAction.type.in_(group_types),
            ).with_for_update()
        )).all()
        for action in earlier:
            action.status = ActionStatus.REJECTED.value
            action.decided_at = now()
            action.decided_by = "system"
            action.decision_reason = "superseded"
            self.audit(action, "SUPERSEDED", actor_user_id)
            ACTIONS.labels(action.type, "superseded").inc()

    async def _expire_if_disabled(self, action: PendingAction) -> None:
        # Covers capabilities off by default, which have no switch row for the kill switch to sweep.
        if action.capability_id in await GovernanceService(self.db).disabled_ids():
            self._expire(action)
            await self.db.commit()
            raise ProblemDetailException(409, "CAPABILITY_DISABLED", "The capability was switched off, so the action expired")

    def _expire(self, action: PendingAction) -> None:
        action.status = ActionStatus.EXPIRED.value
        self.audit(action, "EXPIRED", "system")
        self.announce(action)
        ACTIONS.labels(action.type, "expired").inc()

    @staticmethod
    def _can_see(action: PendingAction, user: RequestUser) -> bool:
        return action.actor_user_id == user.user_id or (
            action.approver_permission is not None and user.has(action.approver_permission)
        )

    @staticmethod
    def _may_decide(action: PendingAction, user: RequestUser) -> bool:
        if action.approver_rule == ApproverRule.SELF.value:
            return action.actor_user_id == user.user_id
        return action.approver_permission is not None and user.has(action.approver_permission)

    def audit(self, action: PendingAction, event: str, by: str, outcome: str | None = None, detail: str | None = None):
        self.db.add(
            ActionAudit(
                pending_action_id=action.id,
                event=event,
                actor_user_id=by,
                on_behalf_of=action.actor_user_id,
                payload_hash=payload_hash(action.payload),
                outcome_code=outcome,
                detail=detail,
                occurred_at=now(),
            )
        )

    def _revision(self, action: PendingAction, author: str) -> None:
        self.db.add(ActionRevision(pending_action_id=action.id, version=action.version, payload=action.payload,
                                   preview=action.preview, author=author, created_at=now()))

    def announce(self, action: PendingAction) -> None:
        if ActionStatus(action.status) not in TERMINAL:
            return
        add_event(
            self.db,
            ACTION_DECIDED,
            aggregate_type="PendingAction",
            aggregate_id=action.id,
            payload={
                "pendingActionId": action.id,
                "type": action.type,
                "status": action.status,
                "actorUserId": action.actor_user_id,
                "decidedBy": action.decided_by,
                "sessionId": action.session_id,
                "errorCode": action.error_code,
                "occurredAt": now().isoformat(),
            },
        )
