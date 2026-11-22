import hashlib

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.problem import ProblemDetailException
from app.entities.recommendation_slate import RecommendationSlate
from app.models.schemas import InteractionRequest
from app.repositories.course_repository import CourseRepository
from app.repositories.interaction_repository import InteractionRepository


# DISMISS and UNDISMISS are kept for the record but never enter the profile.
INTERACTION_WEIGHTS = {"VIEW": 1.0, "CLICK": 1.5, "DISMISS": 0.0, "UNDISMISS": 0.0}
MAX_IDEMPOTENCY_KEY_LENGTH = 255


class InteractionService:
    def __init__(self, db: Session):
        self.db = db
        self.interactions = InteractionRepository(db)
        self.courses = CourseRepository(db)

    def record(self, user_id: str, idempotency_key: str, request: InteractionRequest) -> tuple[dict, bool]:
        """Record one signal; returns the result and whether it replays an earlier request.

        The same key with the same request returns the first result; with
        another request it is refused.
        """
        if len(idempotency_key) > MAX_IDEMPOTENCY_KEY_LENGTH:
            raise ProblemDetailException(400, "VALIDATION_FAILED", "Idempotency-Key must be at most 255 characters")
        fingerprint = _fingerprint(request)
        previous = self.interactions.find_by_idempotency_key(user_id, idempotency_key)
        if previous is not None:
            return _replay(previous, fingerprint), True

        if not self.courses.get_by_ids([request.courseId]):
            raise ProblemDetailException(404, "RESOURCE_NOT_FOUND", "Course not found or not published")
        if request.recommendationId:
            slate = self.db.get(RecommendationSlate, request.recommendationId)
            if slate is None or slate.user_id != user_id or request.courseId not in (slate.course_ids or []):
                raise ProblemDetailException(
                    400,
                    "VALIDATION_FAILED",
                    "recommendationId does not name a recommendation of this course for the caller",
                )

        if request.interactionType == "UNDISMISS":
            self.interactions.remove_interaction(user_id, request.courseId, "DISMISS")
        interaction = self.interactions.add_interaction(
            user_id,
            request.courseId,
            request.interactionType,
            INTERACTION_WEIGHTS[request.interactionType],
            idempotency_key=idempotency_key,
            request_fingerprint=fingerprint,
            recommendation_id=request.recommendationId,
        )
        try:
            self.db.commit()
        except IntegrityError:
            # A concurrent request with the same key won; answer as its replay.
            self.db.rollback()
            previous = self.interactions.find_by_idempotency_key(user_id, idempotency_key)
            if previous is None:
                raise
            return _replay(previous, fingerprint), True
        return _result(interaction), False


def _replay(previous, fingerprint: str) -> dict:
    if previous.request_fingerprint != fingerprint:
        raise ProblemDetailException(
            409,
            "IDEMPOTENCY_KEY_REUSED",
            "This Idempotency-Key was already used with another request",
        )
    return _result(previous)


def _result(interaction) -> dict:
    return {
        "interactionId": interaction.id,
        "courseId": interaction.course_id,
        "interactionType": interaction.interaction_type,
        "recommendationId": interaction.recommendation_id,
        "recordedAt": interaction.created_at.isoformat() if interaction.created_at else None,
    }


def _fingerprint(request: InteractionRequest) -> str:
    payload = f"{request.courseId}\n{request.interactionType}\n{request.recommendationId or ''}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()
