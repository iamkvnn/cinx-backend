from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.agent.registry import Capability, CapabilityRegistry, registry
from app.core.config import settings
from app.governance.entities import AiPolicyAcceptance, CapabilityState, CapabilityUsage


class GovernanceService:
    """Kill switches, per-capability quotas and AI policy acceptance."""

    def __init__(self, db: AsyncSession, capabilities: CapabilityRegistry = registry):
        self.db = db
        self.capabilities = capabilities

    async def disabled_ids(self) -> frozenset[str]:
        overrides = {row.capability_id: row.enabled for row in (await self.db.scalars(select(CapabilityState))).all()}
        return frozenset(
            capability.id
            for capability in self.capabilities.all()
            if not overrides.get(capability.id, capability.enabled_by_default)
        )

    async def set_enabled(self, capability_id: str, enabled: bool, by: str, reason: str | None) -> CapabilityState:
        if self.capabilities.get(capability_id) is None:
            raise ValueError(f"Unknown capability {capability_id}")
        state = await self.db.get(CapabilityState, capability_id)
        if state is None:
            state = CapabilityState(capability_id=capability_id)
            self.db.add(state)
        state.enabled = enabled
        state.updated_by = by
        state.reason = reason
        state.updated_at = datetime.now(timezone.utc)
        await self.db.commit()
        return state

    async def consume_turn(self, user_id: str, capability: Capability) -> bool:
        """Count one turn against the daily quota; False when the quota is spent."""
        if capability.daily_quota is None:
            return True
        today = datetime.now(timezone.utc).date()
        key = {"user_id": user_id, "capability_id": capability.id, "usage_day": today}
        usage = await self.db.get(CapabilityUsage, key, with_for_update=True)
        if usage is None:
            usage = CapabilityUsage(**key, turns=0)
            self.db.add(usage)
        if usage.turns >= capability.daily_quota:
            return False
        usage.turns += 1
        await self.db.commit()
        return True

    async def policy_accepted(self, user_id: str) -> bool:
        return await self.db.get(AiPolicyAcceptance, (user_id, settings.AI_POLICY_VERSION)) is not None

    async def accept_policy(self, user_id: str, version: int) -> AiPolicyAcceptance:
        if version != settings.AI_POLICY_VERSION:
            raise ValueError(f"The current AI policy is version {settings.AI_POLICY_VERSION}")
        existing = await self.db.get(AiPolicyAcceptance, (user_id, version))
        if existing:
            return existing
        acceptance = AiPolicyAcceptance(user_id=user_id, policy_version=version, accepted_at=datetime.now(timezone.utc))
        self.db.add(acceptance)
        await self.db.commit()
        return acceptance
