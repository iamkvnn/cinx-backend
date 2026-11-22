from dataclasses import dataclass

import jwt
from fastapi import Header

from app.core.config import settings
from app.core.problem import ProblemDetailException
from app.agent.auth_claims import AuthorizationClaimError, validate_authorization_claims


@dataclass(frozen=True)
class RequestUser:
    user_id: str
    token: str
    permissions: frozenset[str]


_jwks = jwt.PyJWKClient(settings.KEYCLOAK_JWK_SET_URI, cache_jwk_set=True, lifespan=300)


def _decode_keycloak_jwt(token: str) -> dict:
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


def current_user(authorization: str | None = Header(default=None)) -> RequestUser:
    """The verified caller. Token freshness after a ban or role change is the gateway's check."""
    if not authorization or not authorization.startswith("Bearer "):
        raise ProblemDetailException(401, "UNAUTHORIZED", "A bearer token is required")
    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        raise ProblemDetailException(401, "UNAUTHORIZED", "A bearer token is required")
    try:
        payload = _decode_keycloak_jwt(token)
        user_id = payload.get("sub")
        if not user_id:
            raise jwt.InvalidTokenError("Missing subject")
        raw_permissions = payload.get("permissions") or []
        if not isinstance(raw_permissions, (list, tuple, set)):
            raise jwt.InvalidTokenError("Invalid permissions claim")
        permissions = frozenset(
            value.strip() for value in raw_permissions
            if isinstance(value, str) and value.strip()
        )
    except jwt.PyJWTError as exc:
        raise ProblemDetailException(401, "UNAUTHORIZED", "Invalid access token") from exc
    return RequestUser(user_id=str(user_id), token=token, permissions=permissions)


def optional_user(authorization: str | None = Header(default=None)) -> RequestUser | None:
    """None for a guest (no Authorization header); a header that is there must still be a valid token."""
    if authorization is None:
        return None
    return current_user(authorization)


def require_permission(user: RequestUser, permission: str) -> None:
    if permission not in user.permissions:
        raise ProblemDetailException(403, "FORBIDDEN", "Required permission is missing")
