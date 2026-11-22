from datetime import datetime, timezone

from fastapi import HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.logging import trace_id_var


ERROR_TITLES = {
    "BAD_REQUEST": "Bad request",
    "VALIDATION_FAILED": "Validation failed",
    "UNAUTHORIZED": "Unauthorized",
    "TOKEN_AUTHORIZATION_STALE": "Token authorization stale",
    "FORBIDDEN": "Forbidden",
    "RESOURCE_NOT_FOUND": "Resource not found",
    "CONFLICT": "Conflict",
    "INTERNAL_ERROR": "Internal server error",
    "LLM_UNAVAILABLE": "AI service unavailable",
    "AGENT_EXECUTION_FAILED": "Agent execution failed",
    "AI_POLICY_NOT_ACCEPTED": "AI policy not accepted",
    "CAPABILITY_DISABLED": "Capability disabled",
    "QUOTA_EXCEEDED": "Quota exceeded",
    "ACTION_NOT_PENDING": "Action is not pending",
    "ACTION_EXPIRED": "Action expired",
    "APPROVER_NOT_ALLOWED": "Approver not allowed",
    "SESSION_BUSY": "Session busy",
    "IDEMPOTENCY_KEY_REUSED": "Idempotency key reused",
    "TOKEN_EXCHANGE_FAILED": "Token exchange failed",
    "UPSTREAM_UNAVAILABLE": "Upstream service unavailable",
}


class ProblemDetailException(Exception):
    def __init__(self, status: int, code: str, detail: str, retryable: bool | None = None):
        super().__init__(detail)
        self.status = status
        self.code = code
        self.detail = detail
        self.retryable = retryable


def problem_detail_body(status: int, code: str, detail: str, instance: str, retryable: bool | None = None) -> dict:
    body = {
        "type": f"urn:cinx:problem:{code.lower().replace('_', '-')}",
        "title": ERROR_TITLES.get(code, code.replace("_", " ").title()),
        "status": status,
        "detail": detail,
        "instance": instance,
        "code": code,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "traceId": trace_id_var.get(),
    }
    if retryable is not None:
        body["retryable"] = retryable
    return body


def _problem_response(status: int, body: dict) -> JSONResponse:
    return JSONResponse(status_code=status, media_type="application/problem+json", content=body)


async def problem_detail_exception_handler(request: Request, exc: ProblemDetailException):
    return _problem_response(
        exc.status, problem_detail_body(exc.status, exc.code, exc.detail, request.url.path, exc.retryable)
    )


async def http_exception_handler(request: Request, exc: HTTPException):
    code = {400: "BAD_REQUEST", 401: "UNAUTHORIZED", 403: "FORBIDDEN", 404: "RESOURCE_NOT_FOUND"}.get(
        exc.status_code, "BAD_REQUEST" if exc.status_code < 500 else "INTERNAL_ERROR"
    )
    detail = exc.detail if isinstance(exc.detail, str) else ERROR_TITLES.get(code, "Request failed")
    return _problem_response(exc.status_code, problem_detail_body(exc.status_code, code, detail, request.url.path))


async def validation_exception_handler(request: Request, exc: RequestValidationError):
    body = problem_detail_body(400, "VALIDATION_FAILED", "Validation failed", request.url.path)
    body["errors"] = [
        {
            "field": ".".join(str(part) for part in error.get("loc", []) if part != "body"),
            "message": error.get("msg"),
            "rejectedValue": error.get("input") if isinstance(error.get("input"), (str, int, float, bool)) else None,
        }
        for error in exc.errors()
    ]
    return _problem_response(400, body)
