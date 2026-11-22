"""Prometheus metrics for the learning assistant.

Labels stay low-cardinality: intents, call labels and tool names come from
fixed sets in code, never from user or model text.
"""

from prometheus_client import Counter, Histogram


AGENT_TURNS = Counter(
    "recommendation_agent_turns_total",
    "Agent turns by intent and outcome (completed, failed, cancelled, replayed).",
    ["intent", "outcome"],
)
AGENT_TURN_SECONDS = Histogram(
    "recommendation_agent_turn_duration_seconds",
    "Wall time of one agent turn.",
    ["intent"],
    buckets=(0.5, 1, 2, 4, 8, 15, 30, 60, 90, 120),
)
AGENT_FIRST_TEXT_SECONDS = Histogram(
    "recommendation_agent_time_to_first_text_seconds",
    "Time from the start of a turn to its first text delta.",
    ["intent"],
    buckets=(0.25, 0.5, 1, 2, 4, 8, 15, 30, 60, 90),
)
AGENT_MODEL_CALLS_PER_TURN = Histogram(
    "recommendation_agent_model_calls_per_turn",
    "Model calls made by one agent turn.",
    ["intent"],
    buckets=(0, 1, 2, 3, 4, 5, 6, 8, 10, 15),
)
AGENT_TOOL_CALLS = Counter(
    "recommendation_agent_tool_calls_total",
    "Tool calls by tool and outcome (ok, error, rejected).",
    ["tool", "outcome"],
)
LLM_CALLS = Counter(
    "recommendation_llm_calls_total",
    "Model calls by call label and outcome (ok, error).",
    ["label", "outcome"],
)
LLM_CALL_SECONDS = Histogram(
    "recommendation_llm_call_duration_seconds",
    "Duration of one model call.",
    ["label"],
    buckets=(0.25, 0.5, 1, 2, 4, 8, 15, 30, 60),
)
LLM_TOKENS = Counter(
    "recommendation_llm_tokens_total",
    "Tokens reported by the provider, by call label and direction (input, output).",
    ["label", "direction"],
)
LLM_STREAM_FALLBACKS = Counter(
    "recommendation_llm_stream_fallbacks_total",
    "Turns that switched to non-streamed calls after a stream failed.",
)
EVENTS = Counter(
    "recommendation_events_total",
    "Consumed events by queue and outcome (processed, duplicate, stale, ignored, retried, dead_lettered).",
    ["queue", "outcome"],
)
RAG_REBUILDS = Counter(
    "recommendation_rag_rebuilds_total",
    "Vector and keyword index rebuilds by outcome (ok, error).",
    ["outcome"],
)
RAG_REBUILD_SECONDS = Histogram(
    "recommendation_rag_rebuild_duration_seconds",
    "Duration of one index rebuild.",
    buckets=(0.1, 0.5, 1, 2, 5, 10, 30, 60, 120),
)
