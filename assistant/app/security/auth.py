from dataclasses import dataclass
from datetime import datetime, timezone

import jwt
from anyio import to_thread
from fastapi import Header

from app.core.config import settings
from app.core.problem import ProblemDetailException
from app.security.claims import (
    AuthorizationClaimError,
    mfa_satisfied,
    string_set_claim,
    validate_authorization_claims,
)


@dataclass(frozen=True)
class RequestUser:
    """The verified caller. ``token`` is only ever exchanged, never forwarded."""

    user_id: str
    token: str
    roles: frozenset[str]
    permissions: frozenset[str]
    mfa: bool

    def has(self, permission: str) -> bool:
        return permission in self.permissions


_jwks = jwt.PyJWKClient(settings.KEYCLOAK_JWK_SET_URI, cache_jwk_set=True, lifespan=300)


def decode_user_token(token: str) -> dict:
    signing_key = _jwks.get_signing_key_from_jwt(token)
    payload = jwt.decode(
        token,
        signing_key.key,
        algorithms=["RS256"],
        issuer=settings.KEYCLOAK_ISSUER_URI,
        audience=settings.KEYCLOAK_AUDIENCE,
        options={"require": ["exp", "iat", "iss", "aud", "token_kind"]},
    )
    try:
        validate_authorization_claims(payload)
    except AuthorizationClaimError as exc:
        raise jwt.InvalidTokenError(str(exc)) from exc
    return payload


def user_from_payload(token: str, payload: dict) -> RequestUser:
    user_id = payload.get("sub")
    if not user_id:
        raise jwt.InvalidTokenError("Missing subject")
    return RequestUser(
        user_id=str(user_id),
        token=token,
        roles=string_set_claim(payload, "roles"),
        permissions=string_set_claim(payload, "permissions"),
        mfa=mfa_satisfied(payload, datetime.now(timezone.utc).timestamp()),
    )


async def current_user(authorization: str | None = Header(default=None)) -> RequestUser:
    """The verified caller. Token freshness after a ban or role change is the gateway's check."""
    if not authorization or not authorization.startswith("Bearer "):
        raise ProblemDetailException(401, "UNAUTHORIZED", "A bearer token is required")
    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        raise ProblemDetailException(401, "UNAUTHORIZED", "A bearer token is required")
    try:
        # PyJWKClient fetches keys synchronously on a cache miss.
        payload = await to_thread.run_sync(decode_user_token, token)
        user = user_from_payload(token, payload)
    except jwt.PyJWTError as exc:
        raise ProblemDetailException(401, "UNAUTHORIZED", "Invalid access token") from exc
    return user


def require_permission(user: RequestUser, permission: str) -> None:
    if not user.has(permission):
        raise ProblemDetailException(403, "FORBIDDEN", "Required permission is missing")
