from pydantic import BaseModel, Field


class Citation(BaseModel):
    sourceType: str
    title: str | None = None
    courseId: str | None = None
    lessonIds: list[str] = Field(default_factory=list)
    sectionTitle: str | None = None
    documentId: str | None = None
    sourceUrl: str | None = None
    score: float | None = None
