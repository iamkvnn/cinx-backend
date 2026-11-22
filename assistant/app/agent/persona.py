from enum import Enum

from app.agent.registry import Persona
from app.core.problem import ProblemDetailException
from app.security.auth import RequestUser


class Placement(str, Enum):
    """Where the panel is open. The persona follows the page, never the user's highest role."""

    HOME = "home"
    COURSE_PAGE = "course_page"
    LESSON_PLAYER = "lesson_player"
    LAB_PLAYER = "lab_player"
    LEARNING_PATH = "learning_path"
    INSTRUCTOR_STUDIO = "instructor_studio"
    INSTRUCTOR_COURSE = "instructor_course"
    INSTRUCTOR_GRADING = "instructor_grading"
    INSTRUCTOR_QNA = "instructor_qna"
    ADMIN_COURSE_REVIEW = "admin_course_review"
    ADMIN_MODERATION = "admin_moderation"
    ADMIN_REPORTS = "admin_reports"
    ADMIN_OPERATIONS = "admin_operations"

    @property
    def persona(self) -> Persona:
        if self.value.startswith("instructor_"):
            return Persona.INSTRUCTOR
        if self.value.startswith("admin_"):
            return Persona.ADMIN
        return Persona.LEARNER


def resolve_persona(user: RequestUser, placement: Placement) -> Persona:
    persona = placement.persona
    if not user.has(persona.permission):
        raise ProblemDetailException(403, "FORBIDDEN", f"The {persona.value} assistant is not available to this account")
    return persona
