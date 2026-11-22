"""Prometheus metrics for the assistant.

Labels come from fixed sets in code (persona, capability id, tool name), never
from user or model text, so cardinality stays bounded.
"""

from prometheus_client import Counter, Histogram


TURNS = Counter(
    "assistant_turns_total",
    "Agent turns by persona, capability and outcome (completed, failed, refused).",
    ["persona", "capability", "outcome"],
)
TURN_SECONDS = Histogram(
    "assistant_turn_duration_seconds",
    "Wall time of one agent turn.",
    ["persona"],
    buckets=(0.5, 1, 2, 4, 8, 15, 30, 60, 90, 120),
)
TOOL_CALLS = Counter(
    "assistant_tool_calls_total",
    "Tool calls by capability, tool and outcome (ok, error).",
    ["capability", "tool", "outcome"],
)
TOKENS = Counter(
    "assistant_model_tokens_total",
    "Model tokens by capability and direction (input, output).",
    ["capability", "direction"],
)
TUTOR_MODE_TURNS = Counter(
    "assistant_tutor_mode_turns_total",
    "Lesson tutor turns run in a mode the learner picked, by mode and outcome (completed, failed).",
    ["mode", "outcome"],
)
ANSWER_REPORTS = Counter(
    "assistant_answer_reports_total",
    "Answers learners reported as wrong, by capability and reason.",
    ["capability", "reason"],
)
REMINDER_TEXTS = Counter(
    "assistant_reminder_texts_total",
    "Study reminder rewrites by reason and outcome (written, rejected, timeout, failed, disabled).",
    ["reason", "outcome"],
)
ACTIONS = Counter(
    "assistant_actions_total",
    "Pending actions by type and transition (proposed, approved, rejected, executed, failed, expired).",
    ["type", "transition"],
)
SUGGESTIONS = Counter(
    "assistant_suggestions_total",
    "Background suggestions by kind and outcome (ready, skipped, failed).",
    ["kind", "outcome"],
)
SUGGESTION_OUTCOMES = Counter(
    "assistant_suggestion_outcomes_total",
    "What people did with a ready suggestion, by kind (accepted, edited, dismissed).",
    ["kind", "outcome"],
)
