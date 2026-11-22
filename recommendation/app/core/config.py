from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict
from app.messaging.routing_keys import (
    COMMERCE_ENTITLEMENTS_CHANGED,
    ENROLLMENT_GRANTED,
    LEARNING_PROFILE_UPDATED,
    WISHLIST_ADDED,
    WISHLIST_REMOVED,
)


class Settings(BaseSettings):
    APP_NAME: str = "recommendation"
    APP_ENV: str = "dev"

    DB_HOST: str
    DB_PORT: int
    DB_NAME: str
    DB_USER: str
    DB_PASSWORD: str

    RABBITMQ_HOST: str
    RABBITMQ_PORT: int
    RABBITMQ_USER: str = Field(validation_alias=AliasChoices("RABBITMQ_USERNAME", "RABBITMQ_USER"))
    RABBITMQ_PASSWORD: str
    RABBITMQ_EXCHANGE: str = "learning.events.exchange"
    RABBITMQ_EXCHANGE_TYPE: str = "topic"
    RABBITMQ_QUEUE: str = "recommendation.course.queue"
    RABBITMQ_DLX: str = "dlx.exchange"
    RABBITMQ_DLQ: str = "recommendation.course.dead.queue"
    RABBITMQ_ROUTING_KEYS: str = (
        "course.course.published,course.course.updated,course.course.archived"
    )

    ENROLLMENT_EXCHANGE: str = "learning.events.exchange"
    SOCIAL_EXCHANGE: str = "cinx.domain.events"
    USER_EXCHANGE: str = "user.events.exchange"
    ENROLLMENT_QUEUE: str = "recommendation.enrollment.queue"
    SOCIAL_QUEUE: str = "recommendation.social.queue"
    USER_QUEUE: str = "recommendation.user.queue"
    ENROLLMENT_ROUTING_KEYS: str = ENROLLMENT_GRANTED
    SOCIAL_ROUTING_KEYS: str = f"{WISHLIST_ADDED},{WISHLIST_REMOVED}"
    USER_ROUTING_KEYS: str = f"user.preference.updated,{LEARNING_PROFILE_UPDATED},user.policy.published,user.policy.archived"

    ENTITLEMENT_EXCHANGE: str = "cinx.domain.events"
    ENTITLEMENT_QUEUE: str = "recommendation.subscription-entitlement.queue"
    ENTITLEMENT_ROUTING_KEYS: str = COMMERCE_ENTITLEMENTS_CHANGED
    ENTITLEMENT_DLQ: str = "recommendation.subscription-entitlement.dead.queue"

    # New schema changes are applied by Alembic; runtime bootstrap is rejected.
    SCHEMA_BOOTSTRAP_ENABLED: bool = False

    RECONCILE_BATCH_SIZE: int = 100
    INTERACTION_THRESHOLD_FOR_CONTENT_BASED: int = 3

    DIGITALOCEAN_MODEL_ACCESS_KEY: str
    DIGITALOCEAN_INFERENCE_BASE_URL: str
    DIGITALOCEAN_LLM_MODEL: str
    DIGITALOCEAN_LLM_TIMEOUT_SECONDS: float
    DIGITALOCEAN_LLM_MAX_COMPLETION_TOKENS: int = 4096
    # Transport retries (429/5xx/timeouts) are left to the OpenAI SDK's backoff.
    DIGITALOCEAN_LLM_MAX_RETRIES: int = 2
    # Budget for every model call of one agent turn together.
    AGENT_TURN_DEADLINE_SECONDS: float = 90.0
    AGENT_STREAMING_ENABLED: bool = True
    AGENT_SEARCH_RERANK_ENABLED: bool = False
    # Cosine floor for semantic course candidates; calibrate with evals/search.py.
    AGENT_SEARCH_MIN_SCORE: float = 0.35
    AGENT_POLICY_MIN_SCORE: float = 0.3
    AGENT_MAX_RUNS_PER_IDEMPOTENCY_KEY: int = 3
    AGENT_RUN_RETENTION_DAYS: int = 30
    AGENT_SESSION_RETENTION_DAYS: int = 90
    # Duplicate events are recognised for this long.
    EVENT_INBOX_RETENTION_DAYS: int = 30
    # /recommendations/me slates a later interaction can refer to.
    RECOMMENDATION_SLATE_RETENTION_DAYS: int = 30
    # Worker threads for sync endpoints and SSE streams (anyio's default is 40).
    THREADPOOL_SIZE: int = 100
    EMBEDDING_MODEL_NAME: str = "microsoft/harrier-oss-v1-270m"
    EMBEDDING_QUERY_PROMPT_NAME: str = "web_search_query"
    KEYCLOAK_ISSUER_URI: str = "http://localhost:8180/realms/cinx"
    KEYCLOAK_JWK_SET_URI: str = "http://localhost:8180/realms/cinx/protocol/openid-connect/certs"
    KEYCLOAK_AUDIENCE: str = "cinx-api"
    LEARNING_SERVICE_BASE_URL: str = "http://learning:8084"
    # Public web app; course links in tool output and citations point here.
    WEB_APP_BASE_URL: str = "https://shiny.id.vn"
    AGENT_MAX_SEARCH_RESULTS: int = 8

    # The module's own .env overrides the shared env/local.env.
    model_config = SettingsConfigDict(env_file=("../env/local.env", ".env"), env_file_encoding="utf-8", extra="ignore")


settings = Settings()
