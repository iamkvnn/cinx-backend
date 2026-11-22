from pydantic import BaseModel, Field, AliasChoices
from typing import Optional, List
from datetime import datetime

class LessonPayload(BaseModel):
    id: str
    title: str
    lessonType: Optional[str] = None  # 'VIDEO', 'ARTICLE', 'QUIZ', 'ASSIGNMENT'
    orderIndex: Optional[int] = None
    duration: Optional[int] = 0


class SectionPayload(BaseModel):
    id: str
    title: str
    description: Optional[str] = None
    orderIndex: Optional[int] = None
    duration: Optional[int] = None
    lessons: List[LessonPayload] = Field(default_factory=list)


class CategoryPayload(BaseModel):
    id: Optional[str] = None
    name: Optional[str] = None


class InstructorPayload(BaseModel):
    id: Optional[str] = None
    name: Optional[str] = None
    email: Optional[str] = None
    gender: Optional[str] = None  # 'MALE' | 'FEMALE' | ...
    avatarUrl: Optional[str] = None


class ImagePayload(BaseModel):
    id: Optional[str] = None
    imageUrl: Optional[str] = None


class CoursePayload(BaseModel):
    id: str
    title: str
    description: Optional[str] = None
    category: Optional[CategoryPayload] = None
    instructor: Optional[InstructorPayload] = None
    images: List[ImagePayload] = Field(default_factory=list)
    price: Optional[float] = None
    discountedPrice: Optional[float] = None
    discountRate: Optional[int] = None
    rating: Optional[float] = None
    enrollmentCount: int = 0
    isInSubscription: bool = False
    # Additive access contract.  isInSubscription remains for the compatibility
    # window and is read only when a legacy event omits accessModel.
    accessModel: Optional[str] = None
    subscriptionFeatureKey: Optional[str] = None
    duration: Optional[int] = None
    hasCertificate: bool = False
    certificateTitle: Optional[str] = None
    status: str = "DRAFT"  # 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
    publishStatus: Optional[str] = None  # None | 'WAITING_APPROVAL' | 'PUBLISHED' | 'REJECTED'
    sections: Optional[List[SectionPayload]] = None
    createdAt: Optional[datetime] = None
    updatedAt: Optional[datetime] = None


class CourseEvent(BaseModel):
    course: CoursePayload
    timestamp: Optional[datetime] = None


class EnrolledCourseEvent(BaseModel):
    userId: str
    courseId: str


class WishlistEvent(BaseModel):
    userId: str
    courseId: str
    added: bool


class UserPreferencePayload(BaseModel):
    userId: str
    categoryIds: list[str] = Field(
        default_factory=list,
        validation_alias=AliasChoices("categoryIds", "categorieIds", "categories")
    )

class UserPreferenceEvent(BaseModel):
    payload: UserPreferencePayload

class UserLearningProfileEvent(BaseModel):
    """user.learning-profile.updated v1; recommendation keeps only the goal and the consent."""
    userId: str
    goal: Optional[str] = None
    personalizationConsent: bool = True
    occurredAt: datetime

class PolicyKnowledgeEvent(BaseModel):
    documentId: str
    title: Optional[str] = None
    sourceType: str = "POLICY"
    sourceUrl: Optional[str] = None
    content: Optional[str] = None
    versionNumber: Optional[int] = None
    publishedAt: Optional[datetime] = None

class CourseInteractionPayload(BaseModel):
    userId: str
    courseId: str
    interactionType: str
    ratingValue: Optional[float] = None


class CourseInteractionEvent(BaseModel):
    payload: CourseInteractionPayload
