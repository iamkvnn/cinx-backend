import asyncio
import hashlib
import time

import httpx

from app.core.config import settings
from app.core.problem import ProblemDetailException


GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange"
ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token"


class TokenExchanger:
    """Keycloak standard token exchange (RFC 8693): the user's token for one scoped to a target audience.

    The user's own token never leaves this service. Exchanged tokens live only in
    process memory, keyed by a hash of the subject token, and are never persisted.
    """

    def __init__(self, http: httpx.AsyncClient | None = None, clock=time.monotonic):
        self._http = http
        self._clock = clock
        self._cache: dict[tuple[str, str], tuple[float, str]] = {}
        self._lock = asyncio.Lock()

    async def exchange(self, subject_token: str, audience: str) -> str:
        key = (hashlib.sha256(subject_token.encode()).hexdigest(), audience)
        now = self._clock()
        cached = self._cache.get(key)
        if cached and cached[0] > now:
            return cached[1]
        async with self._lock:
            cached = self._cache.get(key)
            if cached and cached[0] > now:
                return cached[1]
            body = await self._request(subject_token, audience)
            ttl = float(body.get("expires_in", 60)) - settings.TOKEN_EXCHANGE_SKEW_SECONDS
            token = body["access_token"]
            if ttl > 0:
                self._evict(now)
                self._cache[key] = (now + ttl, token)
            return token

    async def _request(self, subject_token: str, audience: str) -> dict:
        form = {
            "grant_type": GRANT_TYPE,
            "client_id": settings.TOKEN_EXCHANGE_CLIENT_ID,
            "client_secret": settings.TOKEN_EXCHANGE_CLIENT_SECRET,
            "subject_token": subject_token,
            "subject_token_type": ACCESS_TOKEN_TYPE,
            "requested_token_type": ACCESS_TOKEN_TYPE,
            "audience": audience,
        }
        client = self._http or httpx.AsyncClient(timeout=settings.AUTH_INTERNAL_TIMEOUT_SECONDS)
        try:
            response = await client.post(settings.KEYCLOAK_TOKEN_URI, data=form)
        except httpx.HTTPError as exc:
            raise ProblemDetailException(502, "TOKEN_EXCHANGE_FAILED", "Identity provider unreachable", retryable=True) from exc
        finally:
            if self._http is None:
                await client.aclose()
        if response.status_code in (400, 401, 403):
            # An expired or revoked subject token: the caller must sign in again.
            raise ProblemDetailException(401, "TOKEN_EXCHANGE_FAILED", "The session can no longer act for this user")
        if response.status_code >= 300:
            raise ProblemDetailException(502, "TOKEN_EXCHANGE_FAILED", "Token exchange failed", retryable=True)
        return response.json()

    def _evict(self, now: float) -> None:
        for key in [key for key, (expires, _) in self._cache.items() if expires <= now]:
            del self._cache[key]


token_exchanger = TokenExchanger()
