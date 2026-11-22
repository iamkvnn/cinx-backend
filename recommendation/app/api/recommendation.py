from typing import Literal

from fastapi import APIRouter, Depends, Header, Query
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from app.agent.auth import RequestUser, current_user, optional_user
from app.agent.services.course_search import MAX_RESULTS, SORTS, CourseSearchService
from app.agent.services.learner_context import load_learner_context
from app.core.database import get_db
from app.repositories.course_repository import CourseFilters
from app.core.problem import ProblemDetailException
from app.models.schemas import InteractionRequest
from app.services.hybrid import RecommendationService
from app.services.interaction_service import InteractionService

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendations"])


@router.get("/me")
def recommend_for_current_user(
    top_k: int = Query(10, ge=1, le=50),
    db: Session = Depends(get_db),
    user: RequestUser = Depends(current_user),
):
    service = RecommendationService(db)
    return {
        "userId": user.user_id,
        **service.recommend_for_user(user.user_id, top_k),
    }


@router.post("/interactions")
def record_interaction(
    request: InteractionRequest,
    db: Session = Depends(get_db),
    user: RequestUser = Depends(current_user),
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key"),
):
    """Record a client signal about a course; the user comes from the token, never the body."""
    if not idempotency_key or not idempotency_key.strip():
        raise ProblemDetailException(400, "VALIDATION_FAILED", "Idempotency-Key is required")
    result, replayed = InteractionService(db).record(user.user_id, idempotency_key.strip(), request)
    return JSONResponse(result, headers={"Idempotent-Replayed": "true"} if replayed else None)


@router.get("/search")
def search_courses(
    query: str = Query(min_length=1, max_length=300),
    category_id: str | None = Query(default=None, alias="categoryId", max_length=64),
    min_price: float | None = Query(default=None, alias="minPrice", ge=0),
    max_price: float | None = Query(default=None, alias="maxPrice", ge=0),
    min_rating: float | None = Query(default=None, alias="minRating", ge=0, le=5),
    has_certificate: bool | None = Query(default=None, alias="hasCertificate"),
    on_sale: bool | None = Query(default=None, alias="onSale"),
    sort: Literal[SORTS] = Query(default="relevance"),
    limit: int = Query(default=5, ge=1, le=MAX_RESULTS),
    db: Session = Depends(get_db),
    user: RequestUser | None = Depends(optional_user),
):
    """Hybrid course search (semantic and BM25 fused) for the catalog, guests included, and the AI assistant.

    Without a token it is a guest search: nothing about a learner is read and the access flags are left out.
    Calls no model unless AGENT_SEARCH_RERANK_ENABLED turns the LLM rerank on.
    """
    learner = load_learner_context(db, user.user_id if user else None)
    filters = CourseFilters(category_id=category_id, price_from=min_price, price_to=max_price, min_rating=min_rating,
                            has_certificate=has_certificate, on_sale=on_sale)
    results, _, applied, topic_matched = CourseSearchService(db, learner=learner).search(query, limit, filters, sort)
    if user is None:
        for result in results:
            result.pop("access", None)
            result.pop("inWishlist", None)
    return {
        "query": query,
        "topicMatched": topic_matched,
        "categoryName": applied.category_name,
        "results": results,
    }
