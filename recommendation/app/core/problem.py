from datetime import datetime, timezone

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi import HTTPException
from fastapi.responses import JSONResponse

from app.core.logging import trace_id_var


ERROR_TITLES = {
    "BAD_REQUEST": "Bad request",
    "VALIDATION_FAILED": "Validation failed",
    "UNAUTHORIZED": "Unauthorized",
    "TOKEN_AUTHORIZATION_STALE": "Token authorization stale",
    "FORBIDDEN": "Forbidden",
    "RESOURCE_NOT_FOUND": "Resource not found",
    "INTERNAL_ERROR": "Internal server error",
    "LLM_UNAVAILABLE": "AI service unavailable",
    "AGENT_EXECUTION_FAILED": "Agent execution failed",
    "ENTITLEMENT_PROJECTION_STALE": "Entitlement projection stale",
    "FEATURE_NOT_ENTITLED": "Feature not entitled",
    "PLAN_NOT_ELIGIBLE": "Plan not eligible",
    "QUOTA_EXCEEDED": "Quota exceeded",
    "QUOTA_RESERVATION_NOT_SETTLEABLE": "Quota reservation cannot be settled",
    "IDEMPOTENCY_KEY_REUSED": "Idempotency key reused",
}


class ProblemDetailException(Exception):
    def __init__(self, status: int, code: str, detail: str, retryable: bool | None = None):
        super().__init__(detail)
        self.status = status
        self.code = code
        self.detail = detail
        self.retryable = retryable


def problem_detail_body(
    status: int,
    code: str,
    detail: str,
    instance: str,
    retryable: bool | None = None,
) -> dict:
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


async def problem_detail_exception_handler(request: Request, exc: ProblemDetailException):
    return JSONResponse(
        status_code=exc.status,
        media_type="application/problem+json",
        content=problem_detail_body(
            status=exc.status,
            code=exc.code,
            detail=exc.detail,
            instance=request.url.path,
            retryable=exc.retryable,
        ),
    )


async def http_exception_handler(request: Request, exc: HTTPException):
    code_by_status = {
        400: "BAD_REQUEST",
        401: "UNAUTHORIZED",
        403: "FORBIDDEN",
        404: "RESOURCE_NOT_FOUND",
    }
    code = code_by_status.get(exc.status_code, "BAD_REQUEST" if exc.status_code < 500 else "INTERNAL_ERROR")
    detail = exc.detail if isinstance(exc.detail, str) else ERROR_TITLES.get(code, "Request failed")
    return JSONResponse(
        status_code=exc.status_code,
        media_type="application/problem+json",
        content=problem_detail_body(
            status=exc.status_code,
            code=code,
            detail=detail,
            instance=request.url.path,
        ),
    )


async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = [
        {
            "field": ".".join(str(part) for part in error.get("loc", []) if part != "body"),
            "message": error.get("msg"),
            "rejectedValue": error.get("input") if isinstance(error.get("input"), (str, int, float, bool)) else None,
        }
        for error in exc.errors()
    ]
    body = problem_detail_body(
        status=400,
        code="VALIDATION_FAILED",
        detail="Validation failed",
        instance=request.url.path,
    )
    body["errors"] = errors
    return JSONResponse(status_code=400, media_type="application/problem+json", content=body)
