from functools import lru_cache
from typing import Iterable

from sentence_transformers import SentenceTransformer

from app.core.config import settings


QUERY_TASK_DESCRIPTION = "Given a search query, retrieve relevant course, lesson, or policy passages"


@lru_cache(maxsize=1)
def get_embedding_model() -> SentenceTransformer:
    return SentenceTransformer(settings.EMBEDDING_MODEL_NAME)


QUERY_CACHE_SIZE = 512


def embed_query(text: str) -> list[float]:
    return list(_cached_query_embedding(str(text or "")))


@lru_cache(maxsize=QUERY_CACHE_SIZE)
def _cached_query_embedding(text: str) -> tuple[float, ...]:
    # Repeated searches and goals skip the model; a tuple keeps the cached value immutable.
    return tuple(embed_texts([text], is_query=True)[0])


def embed_documents(texts: Iterable[str]) -> list[list[float]]:
    return embed_texts(texts, is_query=False)


def embed_texts(texts: Iterable[str], is_query: bool = False) -> list[list[float]]:
    values = [str(text or "") for text in texts]
    model = get_embedding_model()
    if is_query:
        try:
            embeddings = model.encode(values, prompt_name=settings.EMBEDDING_QUERY_PROMPT_NAME)
        except (TypeError, ValueError):
            prompted = [f"Instruct: {QUERY_TASK_DESCRIPTION}\nQuery: {value}" for value in values]
            embeddings = model.encode(prompted)
    else:
        embeddings = model.encode(values)
    return [_to_list(embedding) for embedding in embeddings]


def _to_list(value) -> list[float]:
    if hasattr(value, "tolist"):
        return value.tolist()
    return list(value)
