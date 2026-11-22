from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator

from app.agent.modes import TutorMode
from app.agent.persona import Placement
from app.agent.registry import Capability, Persona
from app.security.auth import RequestUser
from app.tools.http import ServiceClient


class PageContext(BaseModel):
    """What the client says the user is looking at. Claimed, not verified: tools check access."""

    courseId: str | None = Field(default=None, max_length=64)
    lessonId: str | None = Field(default=None, max_length=64)
    positionMs: int | None = Field(default=None, ge=0)
    learningPathId: str | None = Field(default=None, max_length=64)
    submissionId: str | None = Field(default=None, max_length=64)
    reportId: str | None = Field(default=None, max_length=64)
    questionId: str | None = Field(default=None, max_length=64)
    # the quiz lesson a practice job or an instructor request is about
    quizLessonId: str | None = Field(default=None, max_length=64)


class ContextRef(BaseModel):
    """Something the user attached to a question with "@", beside the page itself. Claimed like the page.

    A lab_excerpt is the lines the learner selected in a lab (terminal, file, or a failed check) with their text; the
    text is the learner's own machine output, so it is data, never instructions.
    """

    kind: Literal["lesson", "course", "learning_path", "study_plan", "lab_excerpt"]
    id: str = Field(min_length=1, max_length=64)
    label: str = Field(default="", max_length=120)
    text: str | None = Field(default=None, max_length=4000)
    source: Literal["terminal", "file", "check"] | None = None
    # the file path, or the task or test the check belongs to
    ref: str | None = Field(default=None, max_length=300)

    @model_validator(mode="after")
    def _excerpt_has_text(self) -> "ContextRef":
        if self.kind == "lab_excerpt" and (not self.text or not self.text.strip() or self.source is None):
            raise ValueError("a lab_excerpt needs its text and source")
        if self.kind != "lab_excerpt" and (self.text is not None or self.source is not None):
            raise ValueError("only a lab_excerpt carries text")
        return self


@dataclass
class AgentDeps:
    """Everything a tool may use. The model never sees it; it is passed to tools by the runtime."""

    user: RequestUser
    persona: Persona
    placement: Placement
    context: PageContext
    session_id: str
    available: dict[str, Capability]
    client: ServiceClient
    # (capability) -> whether this turn may use it (kill switch already applied; this is the quota).
    admit: Callable[[Capability], Awaitable[bool]]
    # (type_id, payload) -> the stored pending action, for the model.
    propose: Callable[[str, dict[str, Any]], Awaitable[dict[str, Any]]]
    # (action types, pending only) -> this session's newest draft of those types, {id, type, status, version, payload}, or None
    find_draft: Callable[..., Awaitable[dict[str, Any] | None]] | None = None
    # (action id, payload) -> the draft one version on, {id, type, version, payload, preview}
    revise_draft: Callable[[str, dict[str, Any]], Awaitable[dict[str, Any]]] | None = None
    # (query, source type, top k, namespace or None) -> passages; a namespace keeps a search inside one course
    search_knowledge: Callable[[str, str, int, str | None], Awaitable[list[Any]]] | None = None
    selected: Capability | None = None
    # the page offers one capability, loaded before the model's first step (no select_capability)
    preloaded: bool = False
    # a preloaded turn that called none of its tools was asked once to use them
    nudged: bool = False
    admitted: set[str] = field(default_factory=set)
    # Actions proposed earlier in this session, with their current status.
    session_actions: list[dict[str, Any]] = field(default_factory=list)
    # Learner memory: on only with consent; `memories` are "KIND: text" lines the learner approved.
    memory_enabled: bool = False
    memories: list[str] = field(default_factory=list)
    # (kind, text) -> the pending memory.save, for the model
    propose_memory: Callable[[str, str], Awaitable[dict[str, Any]]] | None = None
    # the tutor mode the learner picked, and how many check-understanding turns came just before this one
    mode: TutorMode | None = None
    mode_step: int = 0
    # what the user attached with "@" (at most two) and the summary of a compacted conversation
    attachments: list[ContextRef] = field(default_factory=list)
    summary: str | None = None
    # the deep links already shown in this turn: one card per place, a few per answer
    shared_links: list[dict[str, Any]] = field(default_factory=list)
