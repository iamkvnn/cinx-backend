from datetime import datetime, timezone


def safe_float(value, default=0.0):
    try:
        return float(value or default)
    except Exception:
        return default


def normalize_rating(rating: float | None) -> float:
    if rating is None:
        return 0.0
    # rating 0..5 => 0..1
    return min(max(rating / 5.0, 0.0), 1.0)


def normalize_enrollment(enrollment: int) -> float:
    # simple saturation
    if enrollment <= 0:
        return 0.0
    if enrollment >= 10000:
        return 1.0
    return enrollment / 10000.0


def freshness_score(created_at) -> float:
    if not created_at:
        return 0.0

    if isinstance(created_at, str):
        try:
            created_at = datetime.fromisoformat(created_at)
        except Exception:
            return 0.0

    now = datetime.now(timezone.utc).replace(tzinfo=None)
    if getattr(created_at, "tzinfo", None) is not None:
        created_at = created_at.replace(tzinfo=None)

    days = (now - created_at).days
    if days <= 30:
        return 1.0
    if days <= 90:
        return 0.7
    if days <= 180:
        return 0.4
    return 0.1
