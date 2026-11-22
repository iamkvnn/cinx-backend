from collections.abc import Callable

from sqlalchemy.ext.asyncio import AsyncSession

from app.actions.entities import PendingAction
from app.actions.service import ActionService, now
from app.actions.types import ActionStatus, ActionTypeRegistry, action_types
from app.core.metrics import ACTIONS
from app.tools.http import ServiceClient, ToolHttpError, service_client


class RetryableExecution(Exception):
    """The target service failed transiently; the workflow step retries with backoff."""


class ActionExecutor:
    """Carries out one approved action exactly as approved. Safe to call again: the target dedupes on the key."""

    def __init__(
        self,
        session_factory: Callable[[], AsyncSession],
        client: ServiceClient = service_client,
        types: ActionTypeRegistry = action_types,
    ):
        self.session_factory = session_factory
        self.client = client
        self.types = types

    async def execute(self, action_id: str, approver_token: str | None) -> str:
        async with self.session_factory() as db:
            action = await db.get(PendingAction, action_id)
            if action is None:
                raise ValueError(f"Pending action {action_id} not found")
            if action.status in (ActionStatus.EXECUTED.value, ActionStatus.FAILED.value):
                return action.status
            if approver_token is None:
                # Recovered after a restart: the approver's token was never stored, so ask again.
                return await self._finish(db, action, ok=False, code="REAPPROVAL_REQUIRED",
                                          detail="The approval was interrupted; approve the action again")
            action.status = ActionStatus.EXECUTING.value
            await db.commit()
            action_type = self.types.get(action.type)
            payload = action_type.payload_model.model_validate(action.payload)
            try:
                result = await action_type.execute(payload, action.idempotency_key, approver_token, self.client)
            except ToolHttpError as exc:
                if exc.retryable:
                    raise RetryableExecution(exc.for_model()) from exc
                return await self._finish(db, action, ok=False, code=exc.code, detail=exc.detail)
            return await self._finish(db, action, ok=True, result=result)

    async def fail(self, action_id: str, code: str, detail: str) -> str:
        async with self.session_factory() as db:
            action = await db.get(PendingAction, action_id)
            if action is None or action.status in (ActionStatus.EXECUTED.value, ActionStatus.FAILED.value):
                return action.status if action else ActionStatus.FAILED.value
            return await self._finish(db, action, ok=False, code=code, detail=detail)

    async def _finish(self, db: AsyncSession, action: PendingAction, *, ok: bool, result: dict | None = None,
                      code: str | None = None, detail: str | None = None) -> str:
        service = ActionService(db, types=self.types)
        action.status = (ActionStatus.EXECUTED if ok else ActionStatus.FAILED).value
        action.result = result
        action.error_code = code
        action.error_detail = detail
        action.executed_at = now()
        service.audit(action, action.status, action.decided_by or "system", outcome=code or "OK", detail=detail)
        service.announce(action)
        await db.commit()
        ACTIONS.labels(action.type, "executed" if ok else "failed").inc()
        return action.status
