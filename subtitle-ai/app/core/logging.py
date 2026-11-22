import contextvars
import json
import logging
import secrets
from datetime import datetime, timezone
from re import fullmatch


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
            "service": "subtitle-ai",
            "logger": record.name,
            "message": record.getMessage(),
            "traceId": trace_id_var.get(),
            "spanId": span_id_var.get(),
            "requestId": request_id_var.get(),
        }
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


def outgoing_trace_headers() -> dict[str, str]:
    trace_id = trace_id_var.get()
    if not trace_id:
        return {}
    return {
        TRACEPARENT_HEADER: f"00-{trace_id}-{span_id_var.get()}-01",
        REQUEST_ID_HEADER: request_id_var.get() or trace_id,
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
