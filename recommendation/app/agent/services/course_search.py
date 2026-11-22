import json
from dataclasses import asdict, replace
from typing import Any

from sqlalchemy.orm import Session

from app.agent.links import course_url
from app.agent.schemas import Citation
from app.agent.services.learner_context import SUBSCRIPTION_ACCESS_MODELS, LearnerContext
from app.core.config import settings
from app.entities.course import Course
from app.repositories.course_repository import CourseFilters, CourseRepository
from app.services.embedding_model import embed_query
from app.services.lexical_index import lexical_course_index
from app.services.llm_client import DigitalOceanLLMClient
from app.services.rag_index import rag_index


RRF_K = 60
CANDIDATE_POOL = 30
RERANK_POOL = 15
# the catalog page shows up to 24; the assistant asks for its default of 5
MAX_RESULTS = 24
RANKER_VERSION = "hybrid-search-v1"


class CourseSearchService:
    def __init__(
        self,
        db: Session,
        llm_client: DigitalOceanLLMClient | None = None,
        learner: LearnerContext | None = None,
    ):
        self.db = db
        self.course_repo = CourseRepository(db)
        self.llm = llm_client or DigitalOceanLLMClient()
        self.learner = learner or LearnerContext()

    def search(
        self,
        query: str,
        top_k: int = 5,
        filters: CourseFilters | None = None,
        sort: str = "relevance",
    ) -> tuple[list[dict], list[Citation], CourseFilters, bool]:
        """Hybrid search: dense and BM25 candidates fused by reciprocal rank.

        Filters stated by the user are hard constraints, applied in code; they
        never make a course relevant.  The last value is False when no course
        matched the topic and the results only satisfy the filters.
        """
        top_k = max(1, min(int(top_k or 5), MAX_RESULTS))
        filters = self.resolve_filters(query, filters or CourseFilters())

        rankings: dict[str, list[str]] = {
            "semantic": list(self._semantic_course_scores(query, CANDIDATE_POOL).keys()),
            "keyword": [course_id for course_id, _ in lexical_course_index.search(query, CANDIDATE_POOL)],
        }
        fused = _reciprocal_rank_fusion(rankings.values())
        if not fused:
            # A literal match of the query text still counts as relevant.
            fused = {
                course.id: 0.0
                for course in self.course_repo.search_published_courses(
                    replace(filters, query=filters.query or query), limit=CANDIDATE_POOL
                )
            }
            rankings["text"] = list(fused)

        courses = [course for course in self.course_repo.get_by_ids(list(fused)) if _matches_filters(course, filters)]
        courses = _sorted(courses, sort)
        results = [
            self._course_to_result(
                course,
                fused[course.id],
                [source for source, ranked in rankings.items() if course.id in ranked],
            )
            for course in courses
        ]
        topic_matched = bool(results)
        if not results and filters.has_structured_filters():
            # Nothing matched the topic: offer courses that satisfy the stated
            # filters, flagged so the answer does not present them as matches.
            results = [
                self._course_to_result(course, 0.0, ["filter"])
                for course in _sorted(self.course_repo.search_published_courses(replace(filters, query=None), limit=CANDIDATE_POOL), sort)
                if _matches_filters(course, filters)
            ]
        if settings.AGENT_SEARCH_RERANK_ENABLED and topic_matched and sort == "relevance" and len(results) > top_k:
            results = self._rerank_filter_results(query, filters, results[:RERANK_POOL])
        results = results[:top_k]
        _rank(results, topic_matched)
        citations = [
            Citation(
                sourceType="course",
                title=result["title"],
                courseId=result["courseId"],
                sourceUrl=course_url(result["courseId"]),
                score=result["score"],
            )
            for result in results
        ]
        return results, citations, filters, topic_matched

    def resolve_filters(self, query: str, filters: CourseFilters) -> CourseFilters:
        """Match the requested category against the catalog; an unknown category is dropped."""
        if not (filters.category_id or filters.category_name):
            return replace(filters, query=filters.query or query)
        category_id, category_name = _resolve_category(
            filters.category_id,
            filters.category_name,
            self.course_repo.list_published_categories(),
        )
        return replace(filters, query=filters.query or query, category_id=category_id, category_name=category_name)

    def _rerank_filter_results(self, query: str, filters: CourseFilters, results: list[dict]) -> list[dict]:
        if len(results) <= 1:
            return results

        candidate_by_id = {result["courseId"]: result for result in results if result.get("courseId")}
        prompt_candidates = [
            {
                "courseId": result["courseId"],
                "title": result.get("title"),
                "description": _short_text(result.get("description"), 500),
                "categoryName": result.get("categoryName"),
                "instructorName": result.get("instructorName"),
                "rating": result.get("rating"),
                "price": result.get("price"),
                "discountedPrice": result.get("discountedPrice"),
                "duration": result.get("duration"),
                "hasCertificate": result.get("hasCertificate"),
            }
            for result in results
        ]
        prompt = f"""
Filter and rerank these course search candidates for relevance to the user request.

Return strict JSON only:
{{
  "courseIds": ["course id to keep, in best order"]
}}

Rules:
- Keep only courses directly relevant to the user request and filters.
- Use only courseIds from candidates.
- Return at most 5 courseIds.
- Return an empty list when none are relevant.

User request:
{query}

Extracted filters:
{json.dumps(filters_to_dict(filters), ensure_ascii=False)}

Candidates:
{json.dumps(prompt_candidates, ensure_ascii=False)}
"""
        try:
            parsed = self.llm.generate_json_required(
                prompt,
                system="You rerank course search candidates. Return strict JSON only.",
                temperature=0.0,
                label="course_search_rerank",
            )
        except Exception:
            return results

        selected_ids = parsed.get("courseIds", parsed.get("course_ids"))
        if not isinstance(selected_ids, list):
            # Unusable output keeps the fused order rather than dropping everything.
            return results
        reranked = []
        seen = set()
        for course_id in selected_ids:
            course_id = str(course_id)
            if course_id in candidate_by_id and course_id not in seen:
                reranked.append(candidate_by_id[course_id])
                seen.add(course_id)
            if len(reranked) >= 5:
                break
        return reranked

    def _semantic_course_scores(self, query: str, top_k: int) -> dict[str, float]:
        """Courses whose best chunk clears the relevance floor, best first."""
        query_embedding = embed_query(query)
        results = rag_index.search(query_embedding, top_k, source_type="course")

        scores: dict[str, float] = {}
        for chunk, score in results:
            if not chunk.course_id or score < settings.AGENT_SEARCH_MIN_SCORE:
                continue
            scores[chunk.course_id] = max(scores.get(chunk.course_id, float("-inf")), score)
        return dict(sorted(scores.items(), key=lambda item: item[1], reverse=True))

    def _course_to_result(self, course: Course, score: float, matched_by: list[str]) -> dict:
        return {
            "courseId": course.id,
            "title": course.title,
            "description": _short_text(course.description, 300),
            "categoryId": course.category_id,
            "categoryName": course.category_name,
            "instructorId": course.instructor_id,
            "instructorName": course.instructor_name,
            "rating": course.rating,
            "enrollmentCount": course.enrollment_count,
            "price": course.price,
            "discountedPrice": course.discounted_price,
            "duration": course.duration,
            "hasCertificate": course.has_certificate,
            "certificateTitle": course.certificate_title,
            "isInSubscription": course.is_in_subscription,
            "access": self.learner.access_for(course),
            "inWishlist": self.learner.in_wishlist(course.id),
            "matchedBy": matched_by,
            "imageUrls": course.image_urls or [],
            "score": round(score, 4),
            "url": course_url(course.id),
        }


def filters_from_arguments(arguments: dict[str, Any]) -> CourseFilters:
    """Structured filters from the course_search tool's arguments."""
    return CourseFilters(
        category_name=_string_or_none(arguments.get("categoryName")),
        min_rating=_number_or_none(arguments.get("minRating")),
        price_to=_number_or_none(arguments.get("maxPrice")),
        has_certificate=_bool_or_none(arguments.get("hasCertificate")),
        is_in_subscription=_bool_or_none(arguments.get("inSubscription")),
        max_duration=_int_or_none(arguments.get("maxDurationMinutes")),
    )


def filters_to_dict(filters: CourseFilters) -> dict:
    return asdict(filters)


def _reciprocal_rank_fusion(rankings) -> dict[str, float]:
    fused: dict[str, float] = {}
    for ranked in rankings:
        for rank, course_id in enumerate(ranked, start=1):
            fused[course_id] = fused.get(course_id, 0.0) + 1.0 / (RRF_K + rank)
    return dict(sorted(fused.items(), key=lambda item: item[1], reverse=True))


def _matches_filters(course: Course, filters: CourseFilters) -> bool:
    if filters.category_id and course.category_id != filters.category_id:
        return False
    if not filters.category_id and filters.category_name:
        wanted = _normalize(filters.category_name)
        actual = _normalize(course.category_name)
        if not actual or (wanted not in actual and actual not in wanted):
            return False
    price = _effective_price(course)
    if filters.price_from is not None and price < filters.price_from:
        return False
    if filters.price_to is not None and price > filters.price_to:
        return False
    if filters.min_rating is not None and (course.rating is None or float(course.rating) < filters.min_rating):
        return False
    if filters.has_certificate is not None and bool(course.has_certificate) != filters.has_certificate:
        return False
    if filters.is_in_subscription is not None:
        in_subscription = bool(course.is_in_subscription) or (course.access_model or "").upper() in SUBSCRIPTION_ACCESS_MODELS
        if in_subscription != filters.is_in_subscription:
            return False
    if filters.max_duration is not None and (course.duration is None or int(course.duration) > filters.max_duration):
        return False
    if filters.on_sale is not None and _on_sale(course) != filters.on_sale:
        return False
    return True


def _on_sale(course: Course) -> bool:
    return course.discounted_price is not None and course.price is not None and float(course.discounted_price) < float(course.price)


# re-orders of the matched set, never a wider one; relevance keeps the fused order
_SORT_KEYS = {
    "rating": lambda c: -float(c.rating or 0),
    "popular": lambda c: -int(c.enrollment_count or 0),
    "priceAsc": lambda c: _effective_price(c),
    "priceDesc": lambda c: -_effective_price(c),
    "newest": lambda c: -(c.created_at.timestamp() if c.created_at else 0.0),
}
SORTS = ("relevance", *_SORT_KEYS)


def _sorted(courses: list[Course], sort: str) -> list[Course]:
    key = _SORT_KEYS.get(sort)
    return sorted(courses, key=key) if key else courses


def _rank(results: list[dict], topic_matched: bool) -> None:
    """Explainable slots: the position, why it is there, and how strongly (both indexes, one, or neither)."""
    for position, result in enumerate(results, start=1):
        sources = set(result["matchedBy"])
        result["rank"] = position
        result["reasonCode"] = "MATCHES_QUERY" if topic_matched else "MATCHES_FILTERS"
        result["scoreBand"] = "HIGH" if {"semantic", "keyword"} <= sources else "MEDIUM" if sources & {"semantic", "keyword"} else "LOW"
        result["rankerVersion"] = RANKER_VERSION


def _effective_price(course: Course) -> float:
    price = course.discounted_price if course.discounted_price is not None else course.price
    return float(price or 0)


def _number_or_none(value) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _int_or_none(value) -> int | None:
    number = _number_or_none(value)
    return int(number) if number is not None else None


def _bool_or_none(value, default=None) -> bool | None:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        if value.lower() in ("true", "yes", "1"):
            return True
        if value.lower() in ("false", "no", "0"):
            return False
    return default


def _string_or_none(value) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _resolve_category(
    category_id: str | None,
    category_name: str | None,
    categories: list[dict[str, str | None]],
) -> tuple[str | None, str | None]:
    by_id = {category["category_id"]: category for category in categories if category.get("category_id")}
    if category_id and category_id in by_id:
        category = by_id[category_id]
        return category.get("category_id"), category.get("category_name")

    if not category_name:
        return None, None

    normalized_name = _normalize(category_name)
    for category in categories:
        if _normalize(category.get("category_name")) == normalized_name:
            return category.get("category_id"), category.get("category_name")
    for category in categories:
        existing = _normalize(category.get("category_name"))
        if normalized_name in existing or existing in normalized_name:
            return category.get("category_id"), category.get("category_name")
    return None, None


def _normalize(value) -> str:
    return " ".join(str(value or "").lower().split())


def _short_text(value: str | None, limit: int) -> str | None:
    if value is None:
        return None
    text = str(value)
    if len(text) <= limit:
        return text
    return text[: limit - 3].rstrip() + "..."
