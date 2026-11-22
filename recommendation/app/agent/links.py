from app.core.config import settings


def course_url(course_id: str) -> str:
    """The public web page of a course."""
    return f"{settings.WEB_APP_BASE_URL.rstrip('/')}/courses/{course_id}"
