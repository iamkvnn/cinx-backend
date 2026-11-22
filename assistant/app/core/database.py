from collections.abc import AsyncIterator

from sqlalchemy import JSON
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from app.core.config import settings


# JSONB in Postgres; plain JSON elsewhere, so unit tests can run on SQLite.
JsonDoc = JSON().with_variant(JSONB(), "postgresql")

engine = create_async_engine(settings.database_url, pool_pre_ping=True, pool_recycle=1800)

SessionLocal = async_sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


async def get_db() -> AsyncIterator[AsyncSession]:
    async with SessionLocal() as db:
        yield db


def import_entities() -> None:
    """Register every mapped class, so Alembic and the mappers see the whole schema."""
    import app.sessions.entities  # noqa: F401
    import app.actions.entities  # noqa: F401
    import app.governance.entities  # noqa: F401
    import app.messaging.entities  # noqa: F401
    import app.billing.entities  # noqa: F401
    import app.knowledge.entities  # noqa: F401
    import app.memory.entities  # noqa: F401
    import app.materials.entities  # noqa: F401
    import app.practice.entities  # noqa: F401
    import app.lab_drafts.entities  # noqa: F401
    import app.suggestions.entities  # noqa: F401
    import app.reports.entities  # noqa: F401
