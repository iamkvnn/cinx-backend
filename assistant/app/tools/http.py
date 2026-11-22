from dataclasses import dataclass
from typing import Any

import httpx

from app.core.config import settings
from app.core.logging import outbound_trace_headers
from app.core.problem import ProblemDetailException
from app.security.token_exchange import TokenExchanger, token_exchanger


@dataclass(frozen=True)
class Upstream:
    """A domain service a tool may call, and the audience its exchanged token carries."""

    name: str
    base_url: str
    audience: str


LEARNING = Upstream("learning", settings.LEARNING_SERVICE_BASE_URL, "cinx-api")
RECOMMENDATION = Upstream("recommendation", settings.RECOMMENDATION_SERVICE_BASE_URL, "cinx-api")
USER = Upstream("user", settings.USER_SERVICE_BASE_URL, "cinx-api")


class ToolHttpError(Exception):
    """An upstream refusal, phrased so the model can correct its next call."""

    def __init__(self, status: int, code: str, detail: str, retryable: bool):
        super().__init__(detail)
        self.status = status
        self.code = code
        self.detail = detail
        self.retryable = retryable

    def for_model(self) -> str:
        hint = "Retry later." if self.retryable else "Do not repeat the same call; change the arguments or tell the user."
        return f"{self.code} ({self.status}): {self.detail}. {hint}"


class ServiceClient:
    """Calls a domain service as the user, with a token exchanged for that service."""

    def __init__(self, exchanger: TokenExchanger = token_exchanger, transport: httpx.AsyncBaseTransport | None = None):
        self._exchanger = exchanger
        self._transport = transport

    async def request(
        self,
        upstream: Upstream,
        method: str,
        path: str,
        user_token: str,
        *,
        params: dict[str, Any] | None = None,
        json: Any = None,
        idempotency_key: str | None = None,
        timeout: float | None = None,
    ) -> Any:
        token = await self._exchanger.exchange(user_token, upstream.audience)
        headers = {"Authorization": f"Bearer {token}", "Accept": "application/json", **outbound_trace_headers()}
        if idempotency_key:
            headers["Idempotency-Key"] = idempotency_key
        async with httpx.AsyncClient(
            base_url=upstream.base_url, timeout=timeout or settings.TOOL_HTTP_TIMEOUT_SECONDS, transport=self._transport
        ) as client:
            try:
                response = await client.request(method, path, params=_drop_none(params), json=json, headers=headers)
            except httpx.HTTPError as exc:
                raise ToolHttpError(503, "UPSTREAM_UNAVAILABLE", f"{upstream.name} is unreachable", True) from exc
        if response.status_code >= 400:
            raise _error_from(upstream, response)
        if response.status_code == 204 or not response.content:
            return None
        body = response.json()
        # Java services wrap success in ApiResponse / PaginatedApiResponse.
        if isinstance(body, dict) and "data" in body and "success" in body:
            return {"data": body["data"], "meta": body["meta"]} if "meta" in body else body["data"]
        return body


def _drop_none(params: dict[str, Any] | None) -> dict[str, Any] | None:
    return None if params is None else {key: value for key, value in params.items() if value is not None}


def _error_from(upstream: Upstream, response: httpx.Response) -> ToolHttpError:
    try:
        body = response.json()
    except ValueError:
        body = {}
    code = str(body.get("code") or ("RESOURCE_NOT_FOUND" if response.status_code == 404 else "UPSTREAM_REJECTED"))
    detail = str(body.get("detail") or body.get("message") or f"{upstream.name} returned {response.status_code}")
    return ToolHttpError(response.status_code, code, detail, response.status_code >= 500 or response.status_code == 429)


def as_problem(error: ToolHttpError) -> ProblemDetailException:
    """For code paths (action execution) that answer the user over HTTP rather than the model."""
    status = error.status if 400 <= error.status < 500 else 502
    return ProblemDetailException(status, error.code, error.detail, retryable=error.retryable)


service_client = ServiceClient()
