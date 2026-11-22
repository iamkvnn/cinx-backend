from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from app.core.config import settings

DATABASE_URL = (
    f"mysql+pymysql://{settings.DB_USER}:{settings.DB_PASSWORD}"
    f"@{settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME}?charset=utf8mb4"
)

engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=3600,
    future=True
)

SessionLocal = sessionmaker(
    bind=engine,
    autoflush=False,
    autocommit=False,
    expire_on_commit=False
)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def ensure_schema():
    # Import all entities so Alembic's metadata/autogeneration and application
    # mappers are complete.  Schema changes are deliberately not applied at
    # process startup: the recommendation service owns its schema through the
    # checked-in Alembic revisions only.
    import app.entities.course  # noqa: F401
    import app.entities.course_chunk  # noqa: F401
    import app.entities.knowledge  # noqa: F401
    import app.entities.user_interaction  # noqa: F401
    import app.entities.user_preference  # noqa: F401
    import app.entities.entitlement  # noqa: F401
    import app.entities.event_inbox  # noqa: F401
    import app.entities.recommendation_slate  # noqa: F401

    if settings.SCHEMA_BOOTSTRAP_ENABLED:
        raise RuntimeError(
            "SCHEMA_BOOTSTRAP_ENABLED is no longer supported; run 'alembic upgrade head' before startup"
        )
