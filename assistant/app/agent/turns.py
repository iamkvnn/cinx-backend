from pydantic_ai.messages import ModelMessage


def this_turn(messages: list[ModelMessage]) -> list[ModelMessage]:
    """The messages after the latest user prompt."""
    latest_prompt = max(
        (i for i, m in enumerate(messages) if any(p.part_kind == "user-prompt" for p in m.parts)), default=-1
    )
    return messages[latest_prompt + 1:]


def called(messages: list[ModelMessage], tool_names: set[str]) -> bool:
    return any(
        part.part_kind == "tool-call" and part.tool_name in tool_names
        for message in messages
        for part in message.parts
    )


def succeeded(messages: list[ModelMessage], tool_name: str) -> bool:
    """Whether a call to tool_name returned normally (not a retry prompt)."""
    return any(
        part.part_kind == "tool-return" and part.tool_name == tool_name
        for message in messages
        for part in message.parts
    )
