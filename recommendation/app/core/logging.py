import contextvars
import json
import logging
import secrets
import time
from datetime import datetime, timezone
from re import fullmatch

from starlette.middleware.base import BaseHTTPMiddleware


TRACEPARENT_HEADER = "traceparent"
REQUEST_ID_HEADER = "X-Request-Id"
trace_id_var = contextvars.ContextVar("trace_id", default="")
span_id_var = contextvars.ContextVar("span_id", default="")
request_id_var = contextvars.ContextVar("request_id", default="")


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "timestamp": datetime.fromtimestamp(record.created, timezone.utc).isoformat(),
            "level": record.levelname,
            "service": "recommendation",
            "logger": record.name,
            "thread": record.threadName,
            "message": record.getMessage(),
            "traceId": trace_id_var.get(),
            "spanId": span_id_var.get(),
            "requestId": request_id_var.get(),
        }
        for field in ("method", "path", "status", "durationMs"):
            value = getattr(record, field, None)
            if value is not None:
                payload[field] = value
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonLogFormatter())
    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    root_logger.addHandler(handler)
    root_logger.setLevel(logging.INFO)
    logging.getLogger("app").setLevel(logging.INFO)


class CorrelationMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        trace_headers = trace_headers_from_values(
            request.headers.get(TRACEPARENT_HEADER),
            request.headers.get(REQUEST_ID_HEADER),
        )
        tokens = set_context(trace_headers)
        started_at = time.perf_counter()
        try:
            response = await call_next(request)
            return response
        finally:
            duration_ms = int((time.perf_counter() - started_at) * 1000)
            status = locals().get("response").status_code if "response" in locals() else 500
            logging.getLogger("app.access").info(
                "HTTP request completed",
                extra={
                    "method": request.method,
                    "path": request.url.path,
                    "status": status,
                    "durationMs": duration_ms,
                },
            )
            if "response" in locals():
                response.headers[TRACEPARENT_HEADER] = trace_headers["traceparent"]
                response.headers[REQUEST_ID_HEADER] = trace_headers["request_id"]
            reset_context(tokens)


def trace_headers_from_values(traceparent: str | None, request_id: str | None) -> dict[str, str]:
    if traceparent and fullmatch(r"00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}", traceparent.strip().lower()):
        trace_id = traceparent.strip().lower()[3:35]
    else:
        trace_id = secrets.token_hex(16)
    span_id = secrets.token_hex(8)
    return {
        "trace_id": trace_id,
        "span_id": span_id,
        "request_id": request_id.strip() if request_id and request_id.strip() else trace_id,
        "traceparent": f"00-{trace_id}-{span_id}-01",
    }


def set_context(headers: dict[str, str]):
    return (
        trace_id_var.set(headers["trace_id"]),
        span_id_var.set(headers["span_id"]),
        request_id_var.set(headers["request_id"]),
    )


def reset_context(tokens) -> None:
    trace_id_var.reset(tokens[0])
    span_id_var.reset(tokens[1])
    request_id_var.reset(tokens[2])
