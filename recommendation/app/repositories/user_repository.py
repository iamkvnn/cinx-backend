from datetime import datetime

from sqlalchemy.orm import Session
from sqlalchemy import select, delete
from app.entities.user_learning_profile import UserLearningProfile
from app.entities.user_preference import UserPreference


class UserRepository:
    def __init__(self, db: Session):
        self.db = db

    def replace_user_preferences(self, user_id: str, categories: list[str]):
        # The caller commits, together with the event's inbox row.
        self.db.execute(delete(UserPreference).where(UserPreference.user_id == user_id))
        for category in categories:
            self.db.add(UserPreference(user_id=user_id, categoryId=category))

    def get_user_categories(self, user_id: str) -> list[str]:
        stmt = select(UserPreference.categoryId).where(UserPreference.user_id == user_id)
        return list(self.db.execute(stmt).scalars().all())

    def get_learning_profile(self, user_id: str) -> UserLearningProfile | None:
        return self.db.get(UserLearningProfile, user_id)

    def upsert_learning_profile(self, user_id: str, goal: str | None, consent: bool, updated_at: datetime) -> bool:
        """Keep the newest snapshot; False when the stored one is newer. The caller commits."""
        stored = self.db.get(UserLearningProfile, user_id)
        if stored is not None and stored.updated_at is not None and updated_at < stored.updated_at:
            return False
        if stored is None:
            self.db.add(UserLearningProfile(user_id=user_id, goal=goal, personalization_consent=consent, updated_at=updated_at))
        else:
            stored.goal = goal
            stored.personalization_consent = consent
            stored.updated_at = updated_at
        return True
