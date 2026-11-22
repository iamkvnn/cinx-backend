import asyncio
import time

import httpx

from app.core.config import settings


# audience -> (expires at, token); each internal API wants its own audience
_tokens: dict[str, tuple[float, str]] = {}
_lock = asyncio.Lock()


async def service_access_token(audience: str = "cinx-auth-internal") -> str:
    """Client-credentials token of cinx-assistant-service, for the auth and learning internal APIs."""
    async with _lock:
        now = time.monotonic()
        cached = _tokens.get(audience)
        if cached and cached[0] > now:
            return cached[1]
        async with httpx.AsyncClient(timeout=settings.AUTH_INTERNAL_TIMEOUT_SECONDS) as client:
            response = await client.post(
                settings.KEYCLOAK_TOKEN_URI,
                data={
                    "grant_type": "client_credentials",
                    "client_id": settings.SERVICE_CLIENT_ID,
                    "client_secret": settings.SERVICE_CLIENT_SECRET,
                    "audience": audience,
                },
            )
            response.raise_for_status()
            body = response.json()
        _tokens[audience] = (now + max(1.0, float(body.get("expires_in", 60)) - 5.0), body["access_token"])
        return body["access_token"]
