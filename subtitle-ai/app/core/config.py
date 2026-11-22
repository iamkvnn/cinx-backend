from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    APP_NAME: str = "subtitle-ai"
    APP_ENV: str = "dev"

    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = Field(
        default="guest",
        validation_alias=AliasChoices("RABBITMQ_USERNAME", "RABBITMQ_USER"),
    )
    RABBITMQ_PASSWORD: str
    LEARNING_EXCHANGE: str = Field(
        default="learning.events.exchange",
        validation_alias=AliasChoices("LEARNING_EXCHANGE", "COURSE_EXCHANGE"),
    )
    AI_SUBTITLE_EXCHANGE: str = "ai.subtitle.events.exchange"
    # Generation requests; translations have their own queue so they never wait behind one.
    SUBTITLE_QUEUE: str = "subtitle-ai.learning.queue"
    SUBTITLE_TRANSLATE_QUEUE: str = "subtitle-ai.learning.translate.queue"
    SUBTITLE_DLQ: str = "subtitle-ai.learning.dead.queue"
    RABBITMQ_DLX: str = "dlx.exchange"
    RABBITMQ_PREFETCH: int = 1
    SUBTITLE_TRANSLATE_PREFETCH: int = 1

    AWS_S3_ENDPOINT: str | None = None
    AWS_S3_BUCKET: str
    AWS_S3_CDN_URL: str
    AWS_REGION: str
    AWS_ACCESS_KEY: str | None = None
    AWS_SECRET_KEY: str | None = None

    DIGITALOCEAN_MODEL_ACCESS_KEY: str | None = None
    DIGITALOCEAN_INFERENCE_BASE_URL: str = "https://inference.do-ai.run/v1"
    DIGITALOCEAN_LLM_MODEL: str
    DIGITALOCEAN_LLM_TIMEOUT_SECONDS: float = 180
    DIGITALOCEAN_LLM_MAX_COMPLETION_TOKENS: int = 4096
    DIGITALOCEAN_LLM_MAX_RETRIES: int = 2
    DIGITALOCEAN_LLM_RETRY_DELAY_SECONDS: float = 10.0

    WHISPER_MODEL_SIZE: str = "large-v3"
    WHISPER_DEVICE: str = "auto"
    WHISPER_COMPUTE_TYPE: str = "auto"
    WHISPER_CPU_THREADS: int = 0
    WHISPER_NUM_WORKERS: int = 1
    WHISPER_BEAM_SIZE: int = 5
    WHISPER_CPU_BEAM_SIZE: int = 5
    WHISPER_PRELOAD: bool = True
    AUDIO_NORMALIZE_ENABLED: bool = True
    SOURCE_LANGUAGE_HINT: str = ""

    MAX_SUBTITLE_LINE_CHARS: int = 52
    MAX_SUBTITLE_LINES: int = 1
    MAX_SUBTITLE_CPS: int = 20
    MIN_SUBTITLE_SECONDS: float = 1.0
    FINAL_SPLIT_BATCH_SIZE: int = 20
    TRANSLATE_BATCH_SIZE: int = 20
    TRANSLATE_CONCURRENCY: int = 4
    USE_LLM_FOR_FINAL_SUBTITLE_SPLIT: bool = False
    WORK_DIR: str = "/tmp/cinx-subtitle-ai"
    MAX_JOB_ATTEMPTS: int = 3
    ARTIFACT_RETENTION_DAYS: int = 7

    # The module's own .env overrides the shared env/local.env.
    model_config = SettingsConfigDict(env_file=("../env/local.env", ".env"), env_file_encoding="utf-8", extra="ignore")

    @property
    def aws_access_key_value(self) -> str | None:
        return self.AWS_ACCESS_KEY

    @property
    def aws_secret_key_value(self) -> str | None:
        return self.AWS_SECRET_KEY


settings = Settings()
