from dataclasses import dataclass
from datetime import datetime

from sqlalchemy.orm import Session
from sqlalchemy import delete, select
from app.entities.user_interaction import UserInteraction


# Client signals that say nothing about what the user likes.
NON_PROFILE_TYPES = frozenset({"DISMISS", "UNDISMISS"})


@dataclass(frozen=True)
class InteractionRow:
    course_id: str
    interaction_type: str
    weight: float
    created_at: datetime | None


class InteractionRepository:
    """Interaction rows; the caller commits, so a projection event and its inbox row share one transaction."""

    def __init__(self, db: Session):
        self.db = db

    def add_interaction(
        self,
        user_id: str,
        course_id: str,
        interaction_type: str,
        weight: float,
        idempotency_key: str | None = None,
        request_fingerprint: str | None = None,
        recommendation_id: str | None = None,
    ) -> UserInteraction:
        interaction = UserInteraction(
            user_id=user_id,
            course_id=course_id,
            interaction_type=interaction_type,
            weight=weight,
            idempotency_key=idempotency_key,
            request_fingerprint=request_fingerprint,
            recommendation_id=recommendation_id,
            created_at=datetime.utcnow(),
        )
        self.db.add(interaction)
        self.db.flush()
        return interaction

    def remove_interaction(self, user_id: str, course_id: str, interaction_type: str) -> int:
        """Delete every row of that type for the user and course; returns how many."""
        result = self.db.execute(
            delete(UserInteraction).where(
                UserInteraction.user_id == user_id,
                UserInteraction.course_id == course_id,
                UserInteraction.interaction_type == interaction_type,
            )
        )
        return result.rowcount or 0

    def find_by_idempotency_key(self, user_id: str, idempotency_key: str) -> UserInteraction | None:
        return self.db.execute(
            select(UserInteraction).where(
                UserInteraction.user_id == user_id,
                UserInteraction.idempotency_key == idempotency_key,
            )
        ).scalar_one_or_none()

    def get_user_interactions(self, user_id: str) -> list[InteractionRow]:
        stmt = select(
            UserInteraction.course_id,
            UserInteraction.interaction_type,
            UserInteraction.weight,
            UserInteraction.created_at,
        ).where(UserInteraction.user_id == user_id)
        return [
            InteractionRow(row.course_id, str(row.interaction_type or "").upper(), float(row.weight or 0.0), row.created_at)
            for row in self.db.execute(stmt)
        ]
