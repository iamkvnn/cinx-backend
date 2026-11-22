import json
import logging
import threading
import time
from dataclasses import dataclass
from types import SimpleNamespace
from typing import Any, Callable, Generator

from app.core.config import settings
from app.core.metrics import LLM_CALL_SECONDS, LLM_CALLS, LLM_STREAM_FALLBACKS, LLM_TOKENS


logger = logging.getLogger(__name__)


LANGUAGE_INSTRUCTION = (
    "Detect the user's language from the latest user message. "
    "Respond in that same language (if a JSON response is required, output ONLY the raw JSON object matching the requested schema with no preamble or postamble, and place all user-facing strings inside the JSON in the user's language). "
    "Write every user-facing string (answer, question, label) in that same language. "
    "Keep reasoning and internal fields (reason, missingFields) in English. "
    "Keep tool names, IDs, enum values, and JSON keys unchanged."
)

_STREAM_FAILURE_EVENTS = {"response.failed", "error"}


class LLMRetryError(Exception):
    def __init__(self, message: str, attempts: int):
        super().__init__(message)
        self.attempts = attempts


@dataclass
class RetryEvent:
    attempt: int
    max_attempts: int
    retry_after_seconds: int
    error: str


_client_lock = threading.Lock()
_shared_clients: dict[tuple, Any] = {}


def _shared_openai_client(base_url: str, api_key: str, timeout: float, max_retries: int):
    """One OpenAI client per configuration, so connections are reused across requests."""
    key = (base_url, api_key, timeout, max_retries)
    with _client_lock:
        client = _shared_clients.get(key)
        if client is None:
            try:
                from openai import OpenAI
            except ImportError as exc:
                raise RuntimeError("openai is not installed") from exc
            client = OpenAI(base_url=base_url, api_key=api_key, timeout=timeout, max_retries=max_retries)
            _shared_clients[key] = client
        return client


class DigitalOceanLLMClient:
    def __init__(
        self,
        max_attempts: int = 3,
        retry_after_seconds: int = 0,
        sleeper: Callable[[int], None] = time.sleep,
    ):
        self.api_key = settings.DIGITALOCEAN_MODEL_ACCESS_KEY
        self.base_url = settings.DIGITALOCEAN_INFERENCE_BASE_URL.rstrip("/")
        self.model = settings.DIGITALOCEAN_LLM_MODEL
        self.timeout = settings.DIGITALOCEAN_LLM_TIMEOUT_SECONDS
        self.max_output_tokens = settings.DIGITALOCEAN_LLM_MAX_COMPLETION_TOKENS
        self.max_retries = settings.DIGITALOCEAN_LLM_MAX_RETRIES
        # Attempts cover unusable output (empty text, invalid JSON, no function
        # call); transport failures are retried by the SDK only.
        self.max_attempts = max_attempts
        self.retry_after_seconds = retry_after_seconds
        self.sleeper = sleeper
        self.retry_events: list[RetryEvent] = []
        self.llm_calls: list[dict[str, Any]] = []
        self.before_call: Callable[[], None] | None = None
        self.streaming_enabled = settings.AGENT_STREAMING_ENABLED
        # Provider requests made since the turn started.
        self.call_count = 0
        self._deadline: float | None = None

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def start_turn(self, deadline_seconds: float, before_call: Callable[[], None] | None = None) -> None:
        """Bound every following call by one turn deadline and run ``before_call`` before each."""
        self._deadline = time.monotonic() + max(1.0, float(deadline_seconds))
        self.before_call = before_call
        self.streaming_enabled = settings.AGENT_STREAMING_ENABLED
        self.call_count = 0

    def clear_retry_events(self) -> None:
        self.retry_events.clear()

    def clear_llm_calls(self) -> None:
        self.llm_calls.clear()

    def generate_json_required(
        self,
        prompt: str,
        system: str,
        temperature: float = 0.0,
        label: str | None = None,
    ) -> dict:
        last_error = "Invalid JSON response."
        started = time.perf_counter()
        response = None
        content = None
        attempts = 0
        for attempt in range(1, self.max_attempts + 1):
            attempts = attempt
            text, error, response = self._generate_text_for_required(
                prompt,
                system=system,
                temperature=temperature,
            )
            if text and not error:
                content = text
                try:
                    parsed = json.loads(_extract_json(text))
                    self._record_llm_call(
                        label,
                        started,
                        prompt,
                        system,
                        content,
                        response,
                        retry_count=attempt - 1,
                    )
                    return parsed
                except json.JSONDecodeError as exc:
                    last_error = f"Invalid JSON response: {exc}"
            else:
                last_error = error or "Empty LLM response."
            self._record_retry_or_sleep(attempt, last_error)
        self._record_llm_call(
            label,
            started,
            prompt,
            system,
            content,
            response,
            retry_count=max(0, attempts - 1),
            error=last_error,
        )
        raise LLMRetryError(last_error, self.max_attempts)

    def call_function_required(
        self,
        input_items: list[dict],
        instructions: str,
        function: dict,
        temperature: float = 0.0,
        label: str | None = None,
    ) -> dict:
        """Force one function call and return its arguments.

        Offering a single tool with ``tool_choice="required"`` is a forced call
        on every OpenAI-compatible provider.  A model that answers in text
        instead is accepted when that text is the JSON object.
        """
        last_error = "The model did not return the required function call."
        for attempt in range(1, self.max_attempts + 1):
            response = self.create_response(
                input_items=input_items,
                instructions=instructions,
                tools=[function],
                tool_choice="required",
                temperature=temperature,
                parallel_tool_calls=False,
                label=label,
            )
            arguments = _function_arguments(response, function["name"])
            if arguments is None:
                arguments = _json_object_or_none(getattr(response, "output_text", "") or "")
            if isinstance(arguments, dict):
                return arguments
            self._record_retry_or_sleep(attempt, last_error)
        raise LLMRetryError(last_error, self.max_attempts)

    def generate_text(
        self,
        prompt: str,
        system: str = "You are a helpful assistant.",
        temperature: float = 0.2,
    ) -> tuple[str | None, str | None]:
        try:
            text, error, _response = self._generate_text_once(prompt, system=system, temperature=temperature)
        except LLMRetryError as exc:
            return None, str(exc)
        return text, error

    def _generate_text_once(
        self,
        prompt: str,
        system: str = "You are a helpful assistant.",
        temperature: float = 0.2,
    ):
        response = self.create_response(
            input_items=[{"role": "user", "content": prompt}],
            instructions=system,
            temperature=temperature,
        )
        try:
            content = response.output_text
        except (ValueError, KeyError, IndexError, TypeError, AttributeError) as exc:
            logger.warning("DigitalOcean LLM response could not be processed: %s", exc)
            return None, "DigitalOcean LLM response could not be processed.", response

        return content, None, response

    def _generate_text_for_required(
        self,
        prompt: str,
        system: str,
        temperature: float,
    ):
        current = getattr(self, "generate_text")
        if getattr(current, "__func__", None) is not DigitalOceanLLMClient.generate_text:
            text, error = current(prompt, system=system, temperature=temperature)
            return text, error, None
        return self._generate_text_once(prompt, system=system, temperature=temperature)

    def create_response(
        self,
        input_items: list[dict],
        instructions: str,
        tools: list[dict] | None = None,
        tool_choice: str | dict | None = None,
        temperature: float = 0.2,
        parallel_tool_calls: bool = False,
        label: str | None = None,
    ):
        kwargs = self._request_kwargs(input_items, instructions, tools, tool_choice, temperature, parallel_tool_calls)
        kwargs["timeout"] = self._prepare_call()
        started = time.perf_counter()
        try:
            response = self._client_for_call(kwargs["timeout"]).responses.create(**kwargs)
        except Exception as exc:
            self._observe_call(label, started, None, failed=True)
            self._record_llm_call(label, started, input_items, instructions, "", None, error=str(exc))
            self._raise_transport_error(exc)
        self._observe_call(label, started, response, failed=False)
        output_text = getattr(response, "output_text", "") or ""
        self._record_llm_call(label, started, input_items, instructions, output_text, response, retry_count=0)
        return response

    def stream_response(
        self,
        input_items: list[dict],
        instructions: str,
        tools: list[dict] | None = None,
        tool_choice: str | dict | None = None,
        temperature: float = 0.2,
        parallel_tool_calls: bool = False,
        label: str | None = None,
    ) -> Generator[str, None, Any]:
        """Yield output text deltas; return the final response object."""
        kwargs = self._request_kwargs(input_items, instructions, tools, tool_choice, temperature, parallel_tool_calls)
        kwargs["timeout"] = self._prepare_call()
        kwargs["stream"] = True
        started = time.perf_counter()
        final = None
        items: list[Any] = []
        text_parts: list[str] = []
        stream = None
        try:
            stream = self._client_for_call(kwargs["timeout"]).responses.create(**kwargs)
            for event in stream:
                event_type = getattr(event, "type", None)
                if event_type == "response.output_text.delta":
                    delta = getattr(event, "delta", "") or ""
                    if delta:
                        text_parts.append(delta)
                        yield delta
                elif event_type == "response.output_item.done":
                    item = getattr(event, "item", None)
                    if item is not None:
                        items.append(item)
                elif event_type in {"response.completed", "response.incomplete"}:
                    final = getattr(event, "response", None)
                elif event_type in _STREAM_FAILURE_EVENTS:
                    raise LLMRetryError(f"LLM stream failed: {_stream_error_message(event)}", 1)
        except LLMRetryError as exc:
            self._observe_call(label, started, None, failed=True)
            self._record_llm_call(label, started, input_items, instructions, "".join(text_parts), None, error=str(exc))
            raise
        except Exception as exc:
            self._observe_call(label, started, None, failed=True)
            self._record_llm_call(label, started, input_items, instructions, "".join(text_parts), None, error=str(exc))
            self._raise_transport_error(exc)
        finally:
            close = getattr(stream, "close", None)
            if callable(close):
                close()
        if final is None or not getattr(final, "output", None):
            final = SimpleNamespace(
                id=getattr(final, "id", None),
                output=items,
                output_text="".join(text_parts),
                usage=getattr(final, "usage", None),
            )
        output_text = getattr(final, "output_text", "") or "".join(text_parts)
        self._observe_call(label, started, final, failed=False)
        self._record_llm_call(label, started, input_items, instructions, output_text, final, retry_count=0)
        return final

    def respond(
        self,
        input_items: list[dict],
        instructions: str,
        tools: list[dict] | None = None,
        tool_choice: str | dict | None = None,
        temperature: float = 0.2,
        parallel_tool_calls: bool = False,
        label: str | None = None,
    ) -> Generator[str, None, Any]:
        """Stream when enabled, otherwise call once; return the final response.

        A stream that fails before its first delta is retried once without
        streaming, and streaming stays off for the rest of the turn.  A stream
        that fails after text was yielded fails the call, since that text has
        already reached the user.
        """
        arguments = dict(
            input_items=input_items,
            instructions=instructions,
            tools=tools,
            tool_choice=tool_choice,
            temperature=temperature,
            parallel_tool_calls=parallel_tool_calls,
            label=label,
        )
        if not self.streaming_enabled:
            return self.create_response(**arguments)
        emitted = False
        stream = self.stream_response(**arguments)
        try:
            while True:
                try:
                    delta = next(stream)
                except StopIteration as completed:
                    return completed.value
                emitted = True
                yield delta
        except LLMRetryError as exc:
            if emitted or self._deadline_passed():
                raise
            logger.warning("LLM streaming failed; continuing this turn without streaming: %s", exc)
            LLM_STREAM_FALLBACKS.inc()
            self.streaming_enabled = False
        return self.create_response(**arguments)

    def _request_kwargs(self, input_items, instructions, tools, tool_choice, temperature, parallel_tool_calls) -> dict:
        kwargs = {
            "model": self.model,
            "instructions": self._with_language_instruction(instructions),
            "input": input_items,
            "temperature": temperature,
            "max_output_tokens": self.max_output_tokens,
            "parallel_tool_calls": parallel_tool_calls,
        }
        if tools is not None:
            kwargs["tools"] = tools
        if tool_choice is not None:
            kwargs["tool_choice"] = tool_choice
        return kwargs

    def _prepare_call(self) -> float:
        """Run the pre-call hook and return this call's timeout within the turn deadline."""
        if not self.is_configured():
            raise LLMRetryError("DigitalOcean model access key not configured.", 1)
        if self.before_call is not None:
            self.before_call()
        timeout = float(self.timeout)
        if self._deadline is not None:
            remaining = self._deadline - time.monotonic()
            if remaining <= 0:
                raise LLMRetryError("Agent turn deadline exceeded.", 1)
            timeout = min(timeout, remaining) if timeout > 0 else remaining
        return timeout

    def _deadline_passed(self) -> bool:
        return self._deadline is not None and time.monotonic() >= self._deadline

    def _raise_transport_error(self, exc: Exception):
        if exc.__class__.__module__.startswith("openai"):
            status_code = getattr(exc, "status_code", None)
            logger.warning("DigitalOcean LLM request failed status=%s error=%s", status_code, exc)
            detail = f" with status {status_code}" if status_code else ""
            raise LLMRetryError(f"DigitalOcean LLM request failed{detail}.", self.max_retries + 1) from exc
        raise exc

    def _get_client(self):
        return _shared_openai_client(self.base_url, self.api_key, float(self.timeout), int(self.max_retries))

    def _client_for_call(self, timeout: float):
        """The shared client, with SDK retries cut so that they cannot outlast the turn deadline.

        Every SDK attempt gets the whole per-call timeout, so only as many
        retries as still fit in the remaining time are allowed.
        """
        client = self._get_client()
        configured = int(self.max_retries)
        if self._deadline is None or timeout <= 0 or configured <= 0:
            return client
        remaining = self._deadline - time.monotonic()
        retries = min(configured, max(0, int(remaining // timeout) - 1))
        return client if retries == configured else client.with_options(max_retries=retries)

    def _record_retry_or_sleep(self, attempt: int, error: str) -> None:
        if attempt >= self.max_attempts:
            return
        self.retry_events.append(
            RetryEvent(
                attempt=attempt,
                max_attempts=self.max_attempts,
                retry_after_seconds=self.retry_after_seconds,
                error=error,
            )
        )
        if self.retry_after_seconds > 0:
            self.sleeper(self.retry_after_seconds)

    def _with_language_instruction(self, system: str) -> str:
        # The fixed instruction goes first, so it belongs to the cacheable prompt prefix.
        return f"{LANGUAGE_INSTRUCTION}\n\n{system}"

    def _observe_call(self, label: str | None, started: float, response, failed: bool) -> None:
        """Count one provider request in the turn and in the process metrics."""
        self.call_count += 1
        metric_label = label or "other"
        LLM_CALLS.labels(metric_label, "error" if failed else "ok").inc()
        LLM_CALL_SECONDS.labels(metric_label).observe(time.perf_counter() - started)
        usage = _usage_dict(response) or {}
        input_tokens = usage.get("input_tokens", usage.get("prompt_tokens"))
        output_tokens = usage.get("output_tokens", usage.get("completion_tokens"))
        if isinstance(input_tokens, (int, float)):
            LLM_TOKENS.labels(metric_label, "input").inc(input_tokens)
        if isinstance(output_tokens, (int, float)):
            LLM_TOKENS.labels(metric_label, "output").inc(output_tokens)

    def _record_llm_call(
        self,
        label: str | None,
        started: float,
        input_value,
        system: str,
        output_text: str | None,
        response,
        retry_count: int = 0,
        error: str | None = None,
    ) -> None:
        if not label:
            return
        self.llm_calls.append(
            {
                "label": label,
                "durationMs": round((time.perf_counter() - started) * 1000, 2),
                "model": self.model,
                "inputCharCount": _char_count(input_value) + _char_count(system),
                "outputCharCount": _char_count(output_text),
                "tokenUsage": _usage_dict(response),
                "retryCount": retry_count,
                "error": error,
            }
        )


def function_calls(response) -> list[dict[str, Any]]:
    """The function calls of a Responses API result, as plain dicts."""
    calls = []
    for item in getattr(response, "output", []) or []:
        data = item if isinstance(item, dict) else getattr(item, "__dict__", {})
        if data.get("type") == "function_call":
            calls.append(
                {
                    "id": data.get("id"),
                    "call_id": data.get("call_id"),
                    "name": data.get("name"),
                    "arguments": data.get("arguments"),
                }
            )
    return calls


def _function_arguments(response, name: str) -> dict | None:
    for call in function_calls(response):
        if call.get("name") != name:
            continue
        arguments = call.get("arguments")
        if isinstance(arguments, dict):
            return arguments
        try:
            parsed = json.loads(arguments or "")
        except (TypeError, json.JSONDecodeError):
            return None
        return parsed if isinstance(parsed, dict) else None
    return None


def _json_object_or_none(text: str) -> dict | None:
    if not text.strip():
        return None
    try:
        parsed = json.loads(_extract_json(text))
    except json.JSONDecodeError:
        return None
    return parsed if isinstance(parsed, dict) else None


def _stream_error_message(event) -> str:
    response = getattr(event, "response", None)
    error = getattr(response, "error", None) if response is not None else None
    return str(getattr(error, "message", None) or getattr(event, "message", None) or "unknown error")


def _extract_json(text: str) -> str:
    cleaned = text.replace("```json", "").replace("```", "").strip()
    if cleaned.startswith("{") and cleaned.endswith("}"):
        return cleaned
    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start != -1 and end != -1 and end > start:
        return cleaned[start:end + 1]
    return cleaned


def _char_count(value) -> int:
    if value is None:
        return 0
    if isinstance(value, str):
        return len(value)
    return len(json.dumps(value, ensure_ascii=False, default=str))


def _usage_dict(response) -> dict | None:
    usage = getattr(response, "usage", None) if response is not None else None
    if usage is None:
        return None
    if isinstance(usage, dict):
        return dict(usage)
    result = {}
    for key in ("input_tokens", "output_tokens", "total_tokens", "prompt_tokens", "completion_tokens"):
        value = getattr(usage, key, None)
        if value is not None:
            result[key] = value
    return result or None
