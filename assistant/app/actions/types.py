from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from datetime import timedelta
from enum import Enum
from typing import Any

from pydantic import BaseModel

from app.tools.http import ServiceClient


class ActionStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    EXECUTING = "EXECUTING"
    EXECUTED = "EXECUTED"
    FAILED = "FAILED"
    REJECTED = "REJECTED"
    EXPIRED = "EXPIRED"


TERMINAL = frozenset({ActionStatus.EXECUTED, ActionStatus.FAILED, ActionStatus.REJECTED, ActionStatus.EXPIRED})


class ApproverRule(str, Enum):
    # The person the agent acted for confirms (one tap): own, reversible writes.
    SELF = "self"
    # Anyone holding approver_permission; the owning service re-checks it on execution.
    PERMISSION = "permission"


Executor = Callable[[BaseModel, str, str, ServiceClient], Awaitable[dict[str, Any]]]
# (stored payload, execution result) -> what the write changed, for the client to refresh and mark.
Changes = Callable[[dict[str, Any], dict[str, Any]], list[dict[str, Any]]]


def change(entity: str, entity_id: Any, op: str, keys: list[str] | None = None) -> dict[str, Any]:
    """One `changed` entry; `keys` are the rows touched inside the entity (lesson ids of a path)."""
    return {"entity": entity, "id": str(entity_id), "op": op, **({"keys": keys} if keys else {})}


@dataclass(frozen=True)
class ActionType:
    """A kind of write the agent may propose, and how code carries it out once approved."""

    id: str
    target_service: str
    payload_model: type[BaseModel]
    approver_rule: ApproverRule
    preview: Callable[[BaseModel], dict[str, Any]]
    # (payload, idempotency_key, approver_token, client) -> result shown to the user and the model.
    execute: Executor
    approver_permission: str | None = None
    requires_mfa: bool = False
    ttl: timedelta = timedelta(hours=72)
    # A new proposal in the session retires pending ones of the same group: the latest draft wins.
    supersede_group: str | None = None
    changes: Changes | None = None
    # (stored payload) -> why it cannot be approved yet (a path still being built), or None
    ready: Callable[[dict[str, Any]], str | None] | None = None

    def __post_init__(self):
        if self.approver_rule is ApproverRule.PERMISSION and not self.approver_permission:
            raise ValueError(f"Action {self.id} needs approver_permission")


class ActionTypeRegistry:
    def __init__(self):
        self._items: dict[str, ActionType] = {}

    def register(self, action_type: ActionType) -> ActionType:
        if action_type.id in self._items:
            raise ValueError(f"Action type {action_type.id} is registered twice")
        self._items[action_type.id] = action_type
        return action_type

    def all(self) -> list[ActionType]:
        return list(self._items.values())

    def get(self, type_id: str) -> ActionType:
        try:
            return self._items[type_id]
        except KeyError as exc:
            raise ValueError(f"Unknown action type {type_id}") from exc


action_types = ActionTypeRegistry()
