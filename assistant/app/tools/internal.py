"""Reads of other services' /internal APIs with the assistant's own service token (admin copilot)."""

from typing import Any

import httpx

from app.core.config import settings
from app.core.logging import outbound_trace_headers
from app.security.service_token import service_access_token
from app.tools.http import ToolHttpError


async def internal_get(base_url: str, audience: str, path: str, params: dict[str, Any] | None = None,
                       transport: httpx.AsyncBaseTransport | None = None) -> Any:
    """GET an internal endpoint; the caller has already checked the admin's permission and MFA."""
    token = await service_access_token(audience)
    async with httpx.AsyncClient(base_url=base_url, transport=transport, timeout=settings.LEARNING_INTERNAL_TIMEOUT_SECONDS) as client:
        try:
            response = await client.get(path, params={k: v for k, v in (params or {}).items() if v is not None},
                                        headers={"Authorization": f"Bearer {token}", "Accept": "application/json",
                                                 **outbound_trace_headers()})
        except httpx.HTTPError as exc:
            raise ToolHttpError(503, "UPSTREAM_UNAVAILABLE", f"{base_url} is unreachable", True) from exc
    if response.status_code >= 400:
        raise ToolHttpError(response.status_code, "UPSTREAM_REFUSED", f"internal call answered {response.status_code}",
                            response.status_code >= 500)
    body = response.json()
    if isinstance(body, dict) and "data" in body and "success" in body:
        return {"data": body["data"], "meta": body["meta"]} if "meta" in body else body["data"]
    return body
