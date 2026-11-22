import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta

import numpy as np

from app.core.config import settings
from app.entities.course import Course
from app.entities.recommendation_slate import RecommendationSlate
from app.repositories.course_repository import CourseRepository
from app.repositories.user_repository import UserRepository
from app.repositories.interaction_repository import NON_PROFILE_TYPES, InteractionRepository, InteractionRow
from app.services.rag_index import rag_index


RANKER_VERSION = "embedding-profile-v1"
# An interaction counts half as much after this many days.
INTERACTION_HALF_LIFE_DAYS = 60
# Course chunks fetched around the user's profile before grouping by course.
SIMILAR_CHUNK_POOL = 100
# A dismissed course stays out of recommendations this long, unless undone.
DISMISS_WINDOW = timedelta(days=90)
# Every source scores 0-100.
HIGH_SCORE = 60
MEDIUM_SCORE = 35
# The interaction that best explains a similarity, strongest first.
SIMILARITY_REASONS = (
    ({"ENROLL", "COMPLETE"}, "SIMILAR_TO_ENROLLED"),
    ({"WISHLIST"}, "SIMILAR_TO_SAVED"),
)


@dataclass(frozen=True)
class Pick:
    course: Course
    score: float
    source: str
    reason: str
    based_on_course_id: str | None = None
    # the learner's goal, when an onboarding pick came from topics chosen with it
    goal: str | None = None


class RecommendationService:
    def __init__(self, db):
        self.db = db
        self.course_repo = CourseRepository(db)
        self.user_repo = UserRepository(db)
        self.interaction_repo = InteractionRepository(db)

    def recommend_for_user(self, user_id: str, top_k: int = 10) -> dict:
        """Ranked picks, each with its reason, and the slate id a later interaction can cite."""
        now = datetime.utcnow()
        interactions = self.interaction_repo.get_user_interactions(user_id)
        positive = [row for row in interactions if row.interaction_type not in NON_PROFILE_TYPES]
        excluded = _excluded_course_ids(interactions, now)

        picks: list[Pick] = []
        profile = self.user_repo.get_learning_profile(user_id)
        # with personalization switched off, the topics and goal the learner gave are not used
        consented = profile is None or profile.personalization_consent
        if consented and len(positive) < settings.INTERACTION_THRESHOLD_FOR_CONTENT_BASED:
            picks = self._onboarding_recommendation(user_id, top_k, excluded, profile.goal if profile else None)
        if not picks:
            picks = self._content_based_recommendation(positive, top_k, excluded, now)
        if not picks:
            picks = self._fallback_popular(top_k, excluded)
        return self._slate(user_id, picks[:top_k], now)

    def _onboarding_recommendation(self, user_id: str, top_k: int, excluded: set[str], goal: str | None = None) -> list[Pick]:
        categories = self.user_repo.get_user_categories(user_id)
        if not categories:
            return []

        courses = self.course_repo.get_published_courses_by_categoryId(categories)

        scored = []
        for c in courses:
            if c.id in excluded:
                continue
            score = 0.0
            if c.category_id in categories:
                score += 50
            score += min((c.rating or 0.0) / 5.0, 1.0) * 30
            score += min((c.enrollment_count or 0) / 1000.0, 1.0) * 20
            scored.append(Pick(c, score, "ONBOARDING", "MATCHES_CATEGORY", goal=goal))

        scored.sort(key=lambda pick: pick.score, reverse=True)
        return scored[:top_k]

    def _content_based_recommendation(
        self,
        interactions: list[InteractionRow],
        top_k: int,
        excluded: set[str],
        now: datetime,
    ) -> list[Pick]:
        """Courses near the user's profile: the weighted, recency-decayed mean of the
        vectors of courses they interacted with, taken from the course index."""
        weights: dict[str, float] = {}
        types: dict[str, set[str]] = {}
        for row in interactions:
            age_days = max(0.0, (now - row.created_at).total_seconds() / 86400) if row.created_at else 0.0
            decayed = (row.weight or 1.0) * 0.5 ** (age_days / INTERACTION_HALF_LIFE_DAYS)
            weights[row.course_id] = weights.get(row.course_id, 0.0) + decayed
            types.setdefault(row.course_id, set()).add(row.interaction_type)

        vectors = {
            course_id: vector
            for course_id in weights
            if weights[course_id] > 0 and (vector := rag_index.course_vector(course_id)) is not None
        }
        if not vectors:
            return []
        profile = sum(vectors[course_id] * weights[course_id] for course_id in vectors)
        norm = float(np.linalg.norm(profile))
        if not norm:
            return []

        similarity: dict[str, float] = {}
        for chunk, score in rag_index.search((profile / norm).tolist(), SIMILAR_CHUNK_POOL, source_type="course"):
            if chunk.course_id and chunk.course_id not in weights and chunk.course_id not in excluded:
                similarity[chunk.course_id] = max(similarity.get(chunk.course_id, -1.0), score)
        if not similarity:
            return []

        picks = []
        for c in self.course_repo.get_by_ids(list(similarity)):
            score = (
                max(0.0, similarity[c.id]) * 80
                + min((c.rating or 0.0) / 5.0, 1.0) * 10
                + min((c.enrollment_count or 0) / 1000.0, 1.0) * 10
            )
            based_on = _closest_course(c.id, vectors) or max(weights, key=weights.get)
            picks.append(Pick(c, float(score), "CONTENT_BASED", _similarity_reason(types.get(based_on, set())), based_on))

        picks.sort(key=lambda pick: pick.score, reverse=True)
        return picks[:top_k]

    def _fallback_popular(self, top_k: int, excluded: set[str]) -> list[Pick]:
        courses = [course for course in self.course_repo.get_published_courses() if course.id not in excluded]

        def score(c):
            freshness = 0
            if c.created_at:
                age_days = max((datetime.now() - c.created_at).days, 0)
                freshness = max(0, 30 - min(age_days, 30)) / 30 * 10

            return min((c.rating or 0.0) / 5.0, 1.0) * 60 + min((c.enrollment_count or 0) / 1000.0, 1.0) * 30 + freshness

        ranked = sorted(courses, key=score, reverse=True)
        return [Pick(c, score(c), "POPULAR_FALLBACK", "POPULAR") for c in ranked[:top_k]]

    def _slate(self, user_id: str, picks: list[Pick], now: datetime) -> dict:
        """Store the slate, so an interaction can be traced back to it, and shape the response."""
        if not picks:
            return {"recommendationId": None, "rankerVersion": RANKER_VERSION, "recommendations": []}
        recommendation_id = str(uuid.uuid4())
        self.db.add(
            RecommendationSlate(
                id=recommendation_id,
                user_id=user_id,
                ranker_version=RANKER_VERSION,
                course_ids=[pick.course.id for pick in picks],
                created_at=now,
            )
        )
        self.db.commit()
        based_on_ids = [pick.based_on_course_id for pick in picks if pick.based_on_course_id]
        titles = {course.id: course.title for course in self.course_repo.get_by_ids(based_on_ids)}
        return {
            "recommendationId": recommendation_id,
            "rankerVersion": RANKER_VERSION,
            "recommendations": [
                _item(pick, rank, recommendation_id, titles) for rank, pick in enumerate(picks, start=1)
            ],
        }


def _item(pick: Pick, rank: int, recommendation_id: str, titles: dict[str, str]) -> dict:
    item = {
        "courseId": pick.course.id,
        "rank": rank,
        "reasonCode": pick.reason,
        "source": pick.source,
        "scoreBand": "HIGH" if pick.score >= HIGH_SCORE else "MEDIUM" if pick.score >= MEDIUM_SCORE else "LOW",
        "recommendationId": recommendation_id,
        "rankerVersion": RANKER_VERSION,
        "course": _card(pick.course),
    }
    if pick.based_on_course_id:
        item["basedOnCourseId"] = pick.based_on_course_id
        if pick.based_on_course_id in titles:
            item["basedOnTitle"] = titles[pick.based_on_course_id]
    if pick.goal:
        item["goal"] = pick.goal
    return item


def _card(course: Course) -> dict:
    """What a course card shows. Learning's `/courses/ids` returns only courses the caller may read, which drops
    every paid course the learner has not bought, so the client draws recommendations from these fields."""
    return {
        "title": course.title,
        "categoryId": course.category_id,
        "categoryName": course.category_name,
        "instructorId": course.instructor_id,
        "instructorName": course.instructor_name,
        "imageUrls": course.image_urls or [],
        "price": course.price,
        "discountedPrice": course.discounted_price,
        "rating": course.rating,
        "enrollmentCount": course.enrollment_count,
        "hasCertificate": bool(course.has_certificate),
        "isInSubscription": bool(course.is_in_subscription),
    }


def _excluded_course_ids(interactions: list[InteractionRow], now: datetime) -> set[str]:
    """Courses the user is enrolled in, and courses dismissed recently."""
    excluded = set()
    for row in interactions:
        if row.interaction_type in {"ENROLL", "COMPLETE"}:
            excluded.add(row.course_id)
        elif row.interaction_type == "DISMISS" and (row.created_at is None or now - row.created_at < DISMISS_WINDOW):
            excluded.add(row.course_id)
    return excluded


def _closest_course(course_id: str, vectors: dict[str, np.ndarray]) -> str | None:
    """The interacted course whose vector is nearest the candidate's."""
    candidate = rag_index.course_vector(course_id)
    if candidate is None:
        return None
    return max(vectors, key=lambda other: float(vectors[other] @ candidate))


def _similarity_reason(interaction_types: set[str]) -> str:
    for types, reason in SIMILARITY_REASONS:
        if interaction_types & types:
            return reason
    return "SIMILAR_TO_VIEWED"
