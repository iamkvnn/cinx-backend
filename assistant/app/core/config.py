from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    APP_NAME: str = "assistant"
    APP_ENV: str = "dev"

    # Prefixed: recommendation reads DB_* from the same env/local.env for its MySQL.
    ASSISTANT_DB_HOST: str = "localhost"
    ASSISTANT_DB_PORT: int = 5433
    ASSISTANT_DB_NAME: str = "assistantdb"
    ASSISTANT_DB_USER: str = "assistant"
    ASSISTANT_DB_PASSWORD: str = ""
    # DBOS keeps its workflow state in its own schema of the same database.
    DBOS_SYSTEM_SCHEMA: str = "dbos"

    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = Field(default="guest", validation_alias=AliasChoices("RABBITMQ_USERNAME", "RABBITMQ_USER"))
    RABBITMQ_PASSWORD: str = ""
    DOMAIN_EVENTS_EXCHANGE: str = "cinx.domain.events"
    OUTBOX_RELAY_INTERVAL_SECONDS: float = 2.0
    OUTBOX_RELAY_BATCH_SIZE: int = 50

    # Any OpenAI-compatible endpoint: vLLM today, a hosted provider later.
    LLM_BASE_URL: str = "http://localhost:8000/v1"
    LLM_API_KEY: str = ""
    LLM_DEFAULT_MODEL: str = "openai/gpt-oss-120b"
    LLM_FAST_MODEL: str = "openai/gpt-oss-20b"
    LLM_TIMEOUT_SECONDS: float = 60.0
    LLM_MAX_OUTPUT_TOKENS: int = 4096
    # Any OpenAI-compatible /v1/embeddings; empty base URL means the LLM endpoint serves it too.
    EMBEDDING_BASE_URL: str = ""
    EMBEDDING_MODEL: str = "bge-m3"
    EMBEDDING_API_KEY: str = ""
    EMBEDDING_TIMEOUT_SECONDS: float = 30.0
    EMBEDDING_BATCH_SIZE: int = 32
    # Cosine floor for a policy passage to count; calibrate with the policy eval suite.
    KNOWLEDGE_MIN_SCORE: float = 0.3
    USER_EVENTS_EXCHANGE: str = "user.events.exchange"
    POLICY_QUEUE: str = "assistant.policy.queue"
    POLICY_DLQ: str = "assistant.policy.dead.queue"
    USER_SERVICE_BASE_URL: str = "http://user:8089"
    AGENT_TURN_DEADLINE_SECONDS: float = 90.0
    # A three-course path made by a model that calls tools one at a time needs about ten requests.
    AGENT_MAX_MODEL_REQUESTS: int = 12
    AGENT_MAX_TOOL_CALLS: int = 16
    AGENT_HISTORY_MESSAGES: int = 40
    # text deltas are written to the turn's event log at most this often, as one event
    AGENT_EVENT_FLUSH_MS: int = 200
    # Credits held before a turn; a ten-request path turn is about 35 (see plan GĐ0-b).
    AGENT_TURN_CREDIT_CAP: int = 60
    # {"model name": {"input": 1.0, "output": 4.0}}; unlisted models use those defaults.
    LLM_MODEL_WEIGHTS: dict[str, dict[str, float]] = {}
    # {"model name": tokens}; wins over the server's max_model_len, which wins over the fallback.
    LLM_MODEL_CONTEXT: dict[str, int] = {}
    LLM_CONTEXT_WINDOW_FALLBACK: int = 32768
    # A conversation compacts before a turn once the last one filled this share of the window.
    AGENT_COMPACT_AT: float = 0.85
    AGENT_COMPACT_KEEP_TURNS: int = 4
    ENTITLEMENT_QUEUE: str = "assistant.subscription-entitlement.queue"
    ENTITLEMENT_DLQ: str = "assistant.subscription-entitlement.dead.queue"
    ENTITLEMENT_ROUTING_KEY: str = "commerce.subscription.entitlements.changed"
    TRANSCRIPT_QUEUE: str = "assistant.transcripts.queue"
    TRANSCRIPT_DLQ: str = "assistant.transcripts.dead.queue"
    TRANSCRIPT_ROUTING_KEY: str = "learning.course.transcripts.changed"
    LEARNING_EVENTS_EXCHANGE: str = "learning.events.exchange"
    SUGGESTION_QUEUE: str = "assistant.suggestions.queue"
    SUGGESTION_DLQ: str = "assistant.suggestions.dead.queue"
    # background suggestions share the GPU with chat: this many run at once, across every instance
    SUGGESTION_CONCURRENCY: int = 1
    SUGGESTION_DEADLINE_SECONDS: float = 300.0
    # weighted-token cap of one run, and of every run of one UTC day on the platform
    SUGGESTION_RUN_CREDIT_CAP: int = 80
    SUGGESTION_DAILY_CREDIT_BUDGET: int = 20000
    # share of a monthly quota one owner may spend in one UTC day
    SUGGESTION_DAILY_SHARE: float = 0.2
    # Events of one course inside this window become one re-index (an instructor fixing several tracks).
    TRANSCRIPT_REINDEX_DELAY_SECONDS: float = 60.0
    # Cues joined into one passage up to about this many characters, each passage starting at its first cue.
    TRANSCRIPT_PASSAGE_CHARS: int = 700
    # Cosine at which a transcript passage counts without sharing a word with the query (app/knowledge/hybrid.py).
    # bge-m3, 2026-10-12: right passages 0.46–0.65, the best passage for a question the course does not cover ≤ 0.47.
    TRANSCRIPT_MIN_SCORE: float = 0.48
    # Jaccard over words and bigrams at which a practice question counts as a repeat (app/practice/duplicates.py).
    PRACTICE_DUPLICATE_JACCARD: float = 0.6
    # One practice question's model call, output retries included; its credit hold lives twice as long.
    PRACTICE_QUESTION_DEADLINE_SECONDS: float = 600.0
    # reasoning_effort of the practice writer; empty keeps the model's default. "none" suits gemma on Ollama
    # (a question in ~10 s instead of 1-3 min of thinking); gpt-oss on vLLM has no "none".
    PRACTICE_REASONING_EFFORT: str = ""
    # One lab draft (up to two model calls); its credit hold lives twice as long.
    LAB_DRAFT_DEADLINE_SECONDS: float = 900.0
    # output tokens per call, reasoning included: a statement, a solution, ten cases and hints
    LAB_DRAFT_MAX_OUTPUT_TOKENS: int = 8192
    LAB_DRAFT_REASONING_EFFORT: str = ""
    # A study reminder's wording: learning waits 5 s and then sends its template, so the model gets less.
    REMINDER_DEADLINE_SECONDS: float = 4.5
    REMINDER_REASONING_EFFORT: str = ""
    LEARNING_INTERNAL_TIMEOUT_SECONDS: float = 20.0
    REPORTING_SERVICE_BASE_URL: str = "http://reporting:8091"
    # read-only observability for the admin operations copilot
    PROMETHEUS_URL: str = "http://prometheus:9090"
    LOKI_URL: str = "http://loki:3100"
    OPS_QUERY_TIMEOUT_SECONDS: float = 15.0
    OPS_MAX_RANGE_HOURS: int = 24
    OPS_MAX_LOG_LINES: int = 500

    KEYCLOAK_ISSUER_URI: str = "http://localhost:8180/realms/cinx"
    KEYCLOAK_JWK_SET_URI: str = "http://localhost:8180/realms/cinx/protocol/openid-connect/certs"
    KEYCLOAK_TOKEN_URI: str = "http://keycloak:8080/realms/cinx/protocol/openid-connect/token"
    # Audience a user token must carry to be accepted here and exchanged.
    KEYCLOAK_AUDIENCE: str = "cinx-assistant"
    # Exchanges user tokens; it has no service account, so it can never mint a token of its own.
    TOKEN_EXCHANGE_CLIENT_ID: str = "cinx-assistant"
    TOKEN_EXCHANGE_CLIENT_SECRET: str = Field(
        default="", validation_alias=AliasChoices("CINX_ASSISTANT_CLIENT_SECRET", "TOKEN_EXCHANGE_CLIENT_SECRET")
    )
    # Service account for the learning, commerce and other internal APIs.
    SERVICE_CLIENT_ID: str = "cinx-assistant-service"
    SERVICE_CLIENT_SECRET: str = Field(
        default="", validation_alias=AliasChoices("CINX_ASSISTANT_SERVICE_CLIENT_SECRET", "SERVICE_CLIENT_SECRET")
    )
    AUTH_INTERNAL_TIMEOUT_SECONDS: float = 2.0
    # Exchanged tokens are cached a little shorter than their own lifetime.
    TOKEN_EXCHANGE_SKEW_SECONDS: float = 10.0

    LEARNING_SERVICE_BASE_URL: str = "http://learning:8084"
    RECOMMENDATION_SERVICE_BASE_URL: str = "http://recommendation:8090"
    # Entitlement snapshot reconciliation when the local projection is missing or its lease ran out.
    COMMERCE_SERVICE_BASE_URL: str = "http://commerce:8087"
    COMMERCE_INTERNAL_TIMEOUT_SECONDS: float = 3.0
    # A user Commerce was just asked about is not asked again before this, whatever it answered.
    ENTITLEMENT_REHYDRATE_COOLDOWN_SECONDS: float = 30.0
    TOOL_HTTP_TIMEOUT_SECONDS: float = 15.0
    WEB_APP_BASE_URL: str = "https://shiny.id.vn"

    # OTLP/HTTP base URL, for example http://tempo:4318; empty turns tracing off.
    OTEL_EXPORTER_OTLP_ENDPOINT: str = ""
    OTEL_TRACES_SAMPLE_RATIO: float = 1.0

    PENDING_ACTION_TTL_HOURS: int = 72
    AI_POLICY_VERSION: int = 1
    THREADPOOL_SIZE: int = 100

    model_config = SettingsConfigDict(env_file=("../env/local.env", ".env"), env_file_encoding="utf-8", extra="ignore")

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.ASSISTANT_DB_USER}:{self.ASSISTANT_DB_PASSWORD}"
            f"@{self.ASSISTANT_DB_HOST}:{self.ASSISTANT_DB_PORT}/{self.ASSISTANT_DB_NAME}"
        )


settings = Settings()
