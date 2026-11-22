from datetime import datetime

from app.repositories.course_repository import CourseRepository
from app.repositories.user_repository import UserRepository
from app.repositories.interaction_repository import InteractionRepository


class SyncService:
    def __init__(self, db):
        self.course_repo = CourseRepository(db)
        self.user_repo = UserRepository(db)
        self.interaction_repo = InteractionRepository(db)

    def handle_course_upsert(self, payload) -> str:
        """Apply a course event: ``changed`` when the search indexes need a rebuild,
        ``unchanged`` otherwise, or ``stale`` for an event older than the stored course."""
        stored = self.course_repo.get(payload.id)
        incoming_at = getattr(payload, "updatedAt", None)
        if stored is not None and stored.updated_at and incoming_at and _wall_time(incoming_at) < _wall_time(stored.updated_at):
            return "stale"

        status_raw = payload.status if hasattr(payload, "status") else "DRAFT"
        status = str(status_raw).upper() if isinstance(status_raw, str) else ("PUBLISHED" if status_raw == 2 else "DRAFT")
        publish_status_raw = getattr(payload, "publishStatus", None)
        publish_status = str(publish_status_raw).upper() if isinstance(publish_status_raw, str) else None

        if status != "PUBLISHED":
            return "changed" if self.course_repo.delete(payload.id) else "unchanged"

        category = payload.category or {}
        instructor = payload.instructor or {}
        images = payload.images or []

        # F1 is additive: prefer the explicit access model, but derive the
        # equivalent model for legacy events that only carry
        # ``isInSubscription``.  This keeps Recommendation dual-read during
        # the event-version compatibility window without opening paid courses.
        access_model = getattr(payload, "accessModel", None)
        if not access_model:
            access_model = "PURCHASE_OR_SUBSCRIPTION" if bool(payload.isInSubscription) else "PURCHASE_ONLY"
        subscription_feature_key = getattr(payload, "subscriptionFeatureKey", None)
        if not subscription_feature_key and access_model in {"SUBSCRIPTION_ONLY", "PURCHASE_OR_SUBSCRIPTION"}:
            subscription_feature_key = "learning.subscription-catalog.access"

        data = {
            "id": payload.id,
            "title": payload.title,
            "description": payload.description,
            "category_id": category.get("id") if isinstance(category, dict) else getattr(category, "id", None),
            "category_name": category.get("name") if isinstance(category, dict) else getattr(category, "name", None),
            "instructor_id": instructor.get("id") if isinstance(instructor, dict) else getattr(instructor, "id", None),
            "instructor_name": instructor.get("name") if isinstance(instructor, dict) else getattr(instructor, "name", None),
            "rating": payload.rating,
            "enrollment_count": payload.enrollmentCount if payload.enrollmentCount is not None else 0,
            "price": payload.price,
            "discounted_price": payload.discountedPrice,
            "discount_rate": payload.discountRate,
            "duration": payload.duration,
            "is_in_subscription": bool(payload.isInSubscription),
            "access_model": access_model,
            "subscription_feature_key": subscription_feature_key,
            "has_certificate": bool(payload.hasCertificate),
            "certificate_title": payload.certificateTitle,
            "image_urls": [
                image.get("imageUrl") if isinstance(image, dict) else getattr(image, "imageUrl", None)
                for image in images
                if (image.get("imageUrl") if isinstance(image, dict) else getattr(image, "imageUrl", None))
            ],
            "status": status,
            "publish_status": publish_status,
            "created_at": payload.createdAt,
            "updated_at": payload.updatedAt,
        }
        if payload.sections is not None:
            data["curriculum"] = [section.model_dump(mode="json") for section in payload.sections]
        return "changed" if self.course_repo.upsert(data) else "unchanged"

    def handle_enrollment(self, user_id: str, course_id: str):
        self.interaction_repo.add_interaction(user_id, course_id, "ENROLL", 5.0)

    def handle_wishlist(self, user_id: str, course_id: str, added: bool):
        if added:
            self.interaction_repo.add_interaction(user_id, course_id, "WISHLIST", 2.0)
        else:
            self.interaction_repo.remove_interaction(user_id, course_id, "WISHLIST")

    def handle_user_preferences(self, payload):
        self.user_repo.replace_user_preferences(payload.userId, payload.categoryIds)

    def handle_learning_profile(self, event) -> bool:
        """Store the goal and consent; False for a snapshot older than the one stored."""
        return self.user_repo.upsert_learning_profile(event.userId, event.goal, event.personalizationConsent,
                                                      _wall_time(event.occurredAt))

    def handle_interaction(self, payload):
        weights = {
            "VIEW": 1.0,
            "WISHLIST": 2.0,
            "ENROLL": 5.0,
            "COMPLETE": 7.0,
            "RATING": 3.0,
        }
        weight = weights.get(payload.interactionType.upper(), 1.0)
        self.interaction_repo.add_interaction(
            user_id=payload.userId,
            course_id=payload.courseId,
            interaction_type=payload.interactionType.upper(),
            weight=weight
        )


def _wall_time(value: datetime) -> datetime:
    # Producers send local wall times; compare them without any zone.
    return value.replace(tzinfo=None)
