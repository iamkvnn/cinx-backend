"""What the assistant knows about the learner, from this service's own projections."""

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.entities.course import Course
from app.entities.entitlement import UserEntitlementProjection, UserFeatureEntitlement
from app.entities.user_interaction import UserInteraction
from app.entities.user_preference import UserPreference
from app.services.entitlement_service import is_projection_fresh


SUBSCRIPTION_ACCESS_MODELS = {"SUBSCRIPTION_ONLY", "PURCHASE_OR_SUBSCRIPTION"}
# Same default as SyncService applies to legacy course events.
DEFAULT_SUBSCRIPTION_FEATURE_KEY = "learning.subscription-catalog.access"
SUMMARY_COURSE_LIMIT = 10


@dataclass(frozen=True)
class LearnerContext:
    user_id: str | None = None
    enrolled_course_ids: frozenset[str] = frozenset()
    wishlist_course_ids: frozenset[str] = frozenset()
    preferred_category_ids: tuple[str, ...] = ()
    subscription_feature_keys: frozenset[str] = frozenset()

    def access_for(self, course: Course) -> str:
        """ENROLLED, SUBSCRIPTION_INCLUDED or PURCHASE_REQUIRED; cinx sells every course, there is no free access.

        Enrollment comes from grant events only, so it is informational: a
        revoked enrollment may still read as ENROLLED.
        """
        if course.id in self.enrolled_course_ids:
            return "ENROLLED"
        access_model = _access_model(course)
        feature_key = course.subscription_feature_key or DEFAULT_SUBSCRIPTION_FEATURE_KEY
        if access_model in SUBSCRIPTION_ACCESS_MODELS and feature_key in self.subscription_feature_keys:
            return "SUBSCRIPTION_INCLUDED"
        return "PURCHASE_REQUIRED"

    def in_wishlist(self, course_id: str) -> bool:
        return course_id in self.wishlist_course_ids


def load_learner_context(db: Session, user_id: str | None) -> LearnerContext:
    if not user_id:
        return LearnerContext()
    interactions = db.execute(
        select(UserInteraction.course_id, UserInteraction.interaction_type).where(
            UserInteraction.user_id == user_id,
            UserInteraction.interaction_type.in_(["ENROLL", "WISHLIST"]),
        )
    ).all()
    preferred = db.execute(
        select(UserPreference.categoryId).where(UserPreference.user_id == user_id)
    ).scalars().all()
    return LearnerContext(
        user_id=user_id,
        enrolled_course_ids=frozenset(course_id for course_id, kind in interactions if kind == "ENROLL"),
        wishlist_course_ids=frozenset(course_id for course_id, kind in interactions if kind == "WISHLIST"),
        preferred_category_ids=tuple(dict.fromkeys(preferred)),
        subscription_feature_keys=_enabled_feature_keys(db, user_id),
    )


def learner_summary(db: Session, learner: LearnerContext) -> dict[str, Any]:
    """A compact profile for the model's instructions."""
    categories: list[str] = []
    if learner.preferred_category_ids:
        rows = db.execute(
            select(Course.category_id, Course.category_name)
            .where(Course.category_id.in_(learner.preferred_category_ids), Course.category_name.isnot(None))
            .distinct()
        ).all()
        names = {category_id: name for category_id, name in rows}
        categories = [names[category_id] for category_id in learner.preferred_category_ids if category_id in names]
    enrolled: list[dict[str, str]] = []
    if learner.enrolled_course_ids:
        rows = db.execute(
            select(Course.id, Course.title)
            .where(Course.id.in_(sorted(learner.enrolled_course_ids)))
            .limit(SUMMARY_COURSE_LIMIT)
        ).all()
        enrolled = [{"courseId": course_id, "title": title} for course_id, title in rows]
    return {
        "preferredCategories": categories,
        "enrolledCourses": enrolled,
        "enrolledCourseCount": len(learner.enrolled_course_ids),
        "wishlistCourseCount": len(learner.wishlist_course_ids),
    }


def _enabled_feature_keys(db: Session, user_id: str) -> frozenset[str]:
    parent = db.get(UserEntitlementProjection, user_id)
    if not is_projection_fresh(parent) or parent.access_status not in {"ACTIVE", "GRACE"}:
        return frozenset()
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    features = db.execute(
        select(UserFeatureEntitlement).where(
            UserFeatureEntitlement.user_id == user_id,
            UserFeatureEntitlement.enabled.is_(True),
        )
    ).scalars().all()
    return frozenset(
        feature.feature_key
        for feature in features
        if feature.value_type == "BOOLEAN"
        and feature.boolean_value
        and (feature.valid_until is None or feature.valid_until > now)
    )


def _access_model(course: Course) -> str:
    if course.access_model:
        return str(course.access_model).upper()
    # Rows projected before the access model existed.
    if course.is_in_subscription:
        return "PURCHASE_OR_SUBSCRIPTION"
    return "PURCHASE_ONLY"
