"""OpenTelemetry traces over OTLP/HTTP; off when no endpoint is configured.

Agent spans carry model, token counts and tool names but never prompt, tool or answer content.
"""

import logging

from fastapi import FastAPI

from app.core.config import settings


logger = logging.getLogger(__name__)


def configure_tracing(app: FastAPI) -> bool:
    if not settings.OTEL_EXPORTER_OTLP_ENDPOINT:
        return False
    from opentelemetry import trace
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
    from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
    from opentelemetry.sdk.resources import Resource
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
    from opentelemetry.sdk.trace.sampling import ParentBasedTraceIdRatio
    from pydantic_ai import Agent
    from pydantic_ai.models.instrumented import InstrumentationSettings

    provider = TracerProvider(
        resource=Resource.create({"service.name": "assistant", "deployment.environment": settings.APP_ENV}),
        sampler=ParentBasedTraceIdRatio(settings.OTEL_TRACES_SAMPLE_RATIO),
    )
    endpoint = settings.OTEL_EXPORTER_OTLP_ENDPOINT.rstrip("/")
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=f"{endpoint}/v1/traces")))
    trace.set_tracer_provider(provider)

    FastAPIInstrumentor.instrument_app(app, excluded_urls="health/.*,metrics")
    HTTPXClientInstrumentor().instrument()
    Agent.instrument_all(InstrumentationSettings(tracer_provider=provider, include_content=False,
                                                 include_binary_content=False))
    logger.info("Tracing to %s", endpoint)
    return True


def current_trace_id() -> str | None:
    """The active OpenTelemetry trace id, so logs and traces share one id."""
    try:
        from opentelemetry import trace
    except ImportError:
        return None
    context = trace.get_current_span().get_span_context()
    return f"{context.trace_id:032x}" if context.is_valid else None
