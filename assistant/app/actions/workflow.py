"""Durable execution of approved actions with DBOS.

The workflow input is the action id only. The approver's token stays in this
process's memory for the few seconds the call takes; a workflow DBOS recovers
after a crash finds no token and ends the action as REAPPROVAL_REQUIRED.
"""

from dbos import DBOS, SetWorkflowID

from app.actions.execution import ActionExecutor, RetryableExecution
from app.core.database import SessionLocal


_executor = ActionExecutor(SessionLocal)
_tokens: dict[str, str] = {}


@DBOS.step(
    retries_allowed=True,
    max_attempts=4,
    interval_seconds=1.0,
    backoff_rate=2.0,
    should_retry=lambda exc: isinstance(exc, RetryableExecution),
)
async def execute_step(action_id: str) -> str:
    return await _executor.execute(action_id, _tokens.get(action_id))


@DBOS.step()
async def fail_step(action_id: str, detail: str) -> str:
    return await _executor.fail(action_id, "UPSTREAM_UNAVAILABLE", detail)


@DBOS.workflow()
async def execute_action_workflow(action_id: str) -> str:
    try:
        return await execute_step(action_id)
    except Exception as exc:
        return await fail_step(action_id, str(exc)[:500])


async def run_action(action_id: str, approver_token: str) -> None:
    """The ExecutionRunner used by the API: one workflow per action, keyed by its id."""
    _tokens[action_id] = approver_token
    try:
        with SetWorkflowID(f"action-{action_id}"):
            handle = await DBOS.start_workflow_async(execute_action_workflow, action_id)
        await handle.get_result()
    finally:
        _tokens.pop(action_id, None)
