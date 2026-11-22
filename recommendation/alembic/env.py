"""Alembic environment for the recommendation-owned schema."""

from logging.config import fileConfig
import os

from alembic import context
from sqlalchemy import engine_from_config, pool

from app.core.database import Base, DATABASE_URL
import app.entities.course  # noqa: F401
import app.entities.course_chunk  # noqa: F401
import app.entities.knowledge  # noqa: F401
import app.entities.user_interaction  # noqa: F401
import app.entities.user_preference  # noqa: F401
import app.entities.entitlement  # noqa: F401
import app.entities.event_inbox  # noqa: F401
import app.entities.recommendation_slate  # noqa: F401

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)
config.set_main_option("sqlalchemy.url", os.getenv("RECOMMENDATION_DATABASE_URL", DATABASE_URL))
target_metadata = Base.metadata


def run_migrations_offline() -> None:
    context.configure(url=config.get_main_option("sqlalchemy.url"), target_metadata=target_metadata, literal_binds=True)
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    connectable = engine_from_config(config.get_section(config.config_ini_section, {}), prefix="sqlalchemy.", poolclass=pool.NullPool)
    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
