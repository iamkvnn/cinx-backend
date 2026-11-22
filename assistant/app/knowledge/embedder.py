import math

import httpx

from app.core.config import settings


class EmbeddingError(Exception):
    """The embedding server failed; the caller decides whether to retry."""


class Embedder:
    """OpenAI-compatible /v1/embeddings client (vLLM, Ollama or a hosted provider). Vectors come back unit length."""

    def __init__(self, base_url: str | None = None, model: str | None = None, api_key: str | None = None,
                 transport: httpx.AsyncBaseTransport | None = None):
        self.base_url = (base_url or settings.EMBEDDING_BASE_URL or settings.LLM_BASE_URL).rstrip("/")
        self.model = model or settings.EMBEDDING_MODEL
        self.api_key = api_key if api_key is not None else settings.EMBEDDING_API_KEY
        self.transport = transport

    async def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        headers = {"Authorization": f"Bearer {self.api_key}"} if self.api_key else {}
        vectors: list[list[float]] = []
        async with httpx.AsyncClient(timeout=settings.EMBEDDING_TIMEOUT_SECONDS, transport=self.transport) as client:
            for start in range(0, len(texts), settings.EMBEDDING_BATCH_SIZE):
                batch = texts[start:start + settings.EMBEDDING_BATCH_SIZE]
                try:
                    response = await client.post(f"{self.base_url}/embeddings", headers=headers,
                                                 json={"model": self.model, "input": batch})
                    response.raise_for_status()
                except httpx.HTTPError as exc:
                    raise EmbeddingError(f"Embedding request failed: {exc}") from exc
                data = sorted(response.json()["data"], key=lambda item: item["index"])
                vectors.extend(normalize(item["embedding"]) for item in data)
        return vectors

    async def embed_query(self, text: str) -> list[float]:
        return (await self.embed([text]))[0]


def normalize(vector: list[float]) -> list[float]:
    norm = math.sqrt(sum(value * value for value in vector)) or 1.0
    return [value / norm for value in vector]


def cosine(a: list[float], b: list[float]) -> float:
    """Both vectors are unit length, so the dot product is the cosine."""
    return sum(x * y for x, y in zip(a, b))
