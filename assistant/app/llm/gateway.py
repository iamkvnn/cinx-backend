from enum import Enum
from functools import lru_cache

from pydantic_ai.models import Model
from pydantic_ai.models.openai import OpenAIChatModel
from pydantic_ai.profiles.openai import OpenAIModelProfile
from pydantic_ai.providers.openai import OpenAIProvider
from pydantic_ai.settings import ModelSettings

from app.core.config import settings


class ModelTier(str, Enum):
    """What a capability asks for; which concrete model serves it is configuration."""

    DEFAULT = "default"
    FAST = "fast"


@lru_cache
def _provider() -> OpenAIProvider:
    # Chat Completions, not the Responses API: vLLM and most hosted providers serve it.
    return OpenAIProvider(base_url=settings.LLM_BASE_URL, api_key=settings.LLM_API_KEY or "unused")


@lru_cache
def model_for(tier: ModelTier) -> Model:
    name = settings.LLM_FAST_MODEL if tier is ModelTier.FAST else settings.LLM_DEFAULT_MODEL
    # max_tokens, not max_completion_tokens: vLLM honours both, Ollama only max_tokens
    return OpenAIChatModel(name, provider=_provider(),
                           profile=OpenAIModelProfile(openai_chat_supports_max_completion_tokens=False))


def model_settings() -> ModelSettings:
    return ModelSettings(
        max_tokens=settings.LLM_MAX_OUTPUT_TOKENS,
        temperature=0.2,
        parallel_tool_calls=True,
        timeout=settings.LLM_TIMEOUT_SECONDS,
    )
