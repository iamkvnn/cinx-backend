from collections.abc import Callable
from dataclasses import dataclass, field
from enum import Enum
from typing import Any

from pydantic_ai import FunctionToolset
from pydantic_ai.messages import ModelMessage

from app.llm.gateway import ModelTier


class Persona(str, Enum):
    LEARNER = "learner"
    INSTRUCTOR = "instructor"
    ADMIN = "admin"

    @property
    def permission(self) -> str:
        return f"assistant.agent.use.{self.value}"


class RiskTier(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class LegalClass(str, Enum):
    STANDARD = "standard"
    # Decision 33/2026 may class it as high risk; stays off until legal signs it off.
    HIGH_RISK_CANDIDATE = "high_risk_candidate"


@dataclass(frozen=True)
class Correction:
    """A correction for the model, and the answer to keep instead when no retry is left (rather than failing the turn)."""

    message: str
    fallback: str


@dataclass(frozen=True)
class Capability:
    """One thing the assistant can do, declared in code; the model can choose it but never change it."""

    id: str
    persona: Persona
    title: str
    description: str
    instructions: str
    toolset: FunctionToolset
    placements: frozenset[str]
    permissions: frozenset[str] = frozenset()
    action_types: frozenset[str] = frozenset()
    model_tier: ModelTier = ModelTier.DEFAULT
    risk: RiskTier = RiskTier.LOW
    legal_class: LegalClass = LegalClass.STANDARD
    daily_quota: int | None = None
    enabled_by_default: bool = True
    eval_suite: str = ""
    # (this turn's messages, the run's deps) -> a correction for the model when the turn ended before its job was done.
    completion_check: Callable[[list[ModelMessage], Any], str | None] | None = None
    # (the answer, the conversation's messages, history included) -> a correction when the answer says what it must not.
    answer_check: Callable[[str, list[ModelMessage]], str | Correction | None] | None = None
    # A capability whose job takes longer (a path built phase by phase) raises the turn's caps; None keeps the defaults.
    max_model_requests: int | None = None
    max_tool_calls: int | None = None
    deadline_seconds: float | None = None

    @property
    def writes(self) -> bool:
        return bool(self.action_types)


@dataclass
class CapabilityRegistry:
    _items: dict[str, Capability] = field(default_factory=dict)

    def register(self, capability: Capability) -> Capability:
        if capability.id in self._items:
            raise ValueError(f"Capability {capability.id} is registered twice")
        if not capability.id.startswith(f"{capability.persona.value}."):
            raise ValueError(f"Capability {capability.id} must be namespaced by its persona")
        if capability.legal_class is LegalClass.HIGH_RISK_CANDIDATE and capability.enabled_by_default:
            raise ValueError(f"Capability {capability.id} needs legal sign-off before it may be on by default")
        self._items[capability.id] = capability
        return capability

    def get(self, capability_id: str) -> Capability | None:
        return self._items.get(capability_id)

    def all(self) -> list[Capability]:
        return list(self._items.values())

    def available(
        self,
        persona: Persona,
        placement: str,
        permissions: frozenset[str],
        disabled: frozenset[str],
    ) -> list[Capability]:
        """What this caller may use here. A capability left out is invisible to the model."""
        return [
            capability
            for capability in self._items.values()
            if capability.persona is persona
            and placement in capability.placements
            and capability.permissions <= permissions
            and capability.id not in disabled
        ]


registry = CapabilityRegistry()
