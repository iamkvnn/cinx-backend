"""Prometheus metrics for the subtitle worker.

Labels stay low-cardinality: job types, stages, LLM tasks and reject reasons
come from fixed sets in code, never from a request.
"""

from prometheus_client import Counter, Gauge, Histogram


# A job can run for hours on CPU.
JOB_BUCKETS = (10, 30, 60, 120, 300, 600, 1200, 1800, 3600, 7200, 14400)

JOBS = Counter(
    "subtitle_jobs_total",
    "Jobs by type and outcome (completed, failed, interrupted, replayed).",
    ["type", "outcome"],
)
JOB_SECONDS = Histogram(
    "subtitle_job_duration_seconds",
    "Wall time of one job attempt that ran to a result.",
    ["type"],
    buckets=JOB_BUCKETS,
)
STAGE_SECONDS = Histogram(
    "subtitle_stage_duration_seconds",
    "Wall time of one pipeline stage.",
    ["stage"],
    buckets=(0.5, 1, 5, 15, 30, 60, 120, 300, 600, 1800, 3600, 7200),
)
JOBS_IN_PROGRESS = Gauge(
    "subtitle_jobs_in_progress",
    "Jobs being processed now.",
    ["type"],
)
JOB_REDELIVERIES = Counter(
    "subtitle_job_redeliveries_total",
    "Deliveries of a job after its first attempt (restarts, lost connections, consumer timeouts).",
    ["type"],
)
ASR_AUDIO_SECONDS = Counter(
    "subtitle_asr_audio_seconds_total",
    "Seconds of audio transcribed.",
)
ASR_REALTIME_FACTOR = Histogram(
    "subtitle_asr_realtime_factor",
    "Transcription time divided by audio duration; below 1 is faster than real time.",
    buckets=(0.05, 0.1, 0.2, 0.3, 0.5, 0.75, 1, 1.5, 2, 3, 5),
)
CUES_PER_JOB = Histogram(
    "subtitle_cues_per_job",
    "Cues in the subtitle a job produced.",
    ["type"],
    buckets=(10, 25, 50, 100, 200, 400, 800, 1600, 3200),
)
LLM_CALLS = Counter(
    "subtitle_llm_calls_total",
    "Model calls by task and outcome (ok, error).",
    ["task", "outcome"],
)
LLM_CALL_SECONDS = Histogram(
    "subtitle_llm_call_duration_seconds",
    "Duration of one model call.",
    ["task"],
    buckets=(0.5, 1, 2, 4, 8, 15, 30, 60, 120, 180),
)
LLM_TOKENS = Counter(
    "subtitle_llm_tokens_total",
    "Tokens reported by the provider, by task and direction (input, output).",
    ["task", "direction"],
)
MESSAGES_REJECTED = Counter(
    "subtitle_messages_rejected_total",
    "Requests rejected to the dead-letter queue, by reason (unidentifiable, publish_failed, unexpected).",
    ["reason"],
)
