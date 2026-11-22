from typing import List, Literal, Optional
from pydantic import BaseModel, Field


class OnboardingRecommendationRequest(BaseModel):
    user_id: Optional[int] = None
    category_ids: List[int]
    level: Optional[str] = None
    language: Optional[str] = None
    top_k: int = 10


class RecommendationItem(BaseModel):
    course_id: str


class RecommendationResponse(BaseModel):
    mode: str
    recommendations: List[RecommendationItem]


class InteractionRequest(BaseModel):
    """A client signal about a course.  There is no user id: it comes from the token."""

    courseId: str = Field(min_length=1, max_length=64)
    interactionType: Literal["VIEW", "CLICK", "DISMISS", "UNDISMISS"]
    # The /recommendations/me slate the course was shown in, when it was.
    recommendationId: Optional[str] = Field(default=None, max_length=64)
