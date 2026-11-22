"""How many tokens each model's context holds, kept per model as coding agents do.

A self-hosted server may serve a model with less than its native window, so the server's own `max_model_len`
(vLLM's `/v1/models`) is what counts; `LLM_MODEL_CONTEXT` overrides it and `LLM_CONTEXT_WINDOW_FALLBACK` covers
a server that does not say.
"""
import asyncio
import logging
import time

import httpx

from app.core.config import settings

log = logging.getLogger(__name__)

# a model the server does not list is asked about again only after this long
_REFRESH_SECONDS = 300.0


class ContextWindows:
    def __init__(self, transport: httpx.AsyncBaseTransport | None = None, clock=time.monotonic) -> None:
        self._known: dict[str, int] = {}
        self._fetched_at: float | None = None
        self._lock = asyncio.Lock()
        self._transport = transport
        self._clock = clock

    async def limit(self, model: str) -> int:
        if model in settings.LLM_MODEL_CONTEXT:
            return settings.LLM_MODEL_CONTEXT[model]
        if model not in self._known and self._due():
            await self._refresh()
        return self._known.get(model, settings.LLM_CONTEXT_WINDOW_FALLBACK)

    def _due(self) -> bool:
        return self._fetched_at is None or self._clock() - self._fetched_at >= _REFRESH_SECONDS

    async def _refresh(self) -> None:
        async with self._lock:
            if not self._due():
                return
            self._fetched_at = self._clock()
            headers = {"Authorization": f"Bearer {settings.LLM_API_KEY}"} if settings.LLM_API_KEY else {}
            try:
                async with httpx.AsyncClient(base_url=settings.LLM_BASE_URL, headers=headers, timeout=5.0,
                                             transport=self._transport) as client:
                    response = await client.get("/models")
                    response.raise_for_status()
                    models = response.json().get("data", [])
            except (httpx.HTTPError, ValueError) as exc:
                log.warning("Model server did not list its models: %s", exc)
                return
            for entry in models:
                size = entry.get("max_model_len") if isinstance(entry, dict) else None
                if isinstance(size, int) and size > 0 and isinstance(entry.get("id"), str):
                    self._known[entry["id"]] = size


context_windows = ContextWindows()
