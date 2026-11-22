class AuthorizationClaimError(ValueError):
    pass


def validate_authorization_claims(payload: dict) -> None:
    """The CINX claims every user token carries; same rules as the Java resource servers."""
    if payload.get("token_kind") != "user":
        raise AuthorizationClaimError("Unexpected token kind")
    try:
        int(payload["iat"])
    except (KeyError, TypeError, ValueError) as exc:
        raise AuthorizationClaimError("Invalid issued-at") from exc


REQUIRED_ACR = "urn:cinx:loa:2"
MFA_MAX_AGE_SECONDS = 15 * 60
APPROVED_MFA_METHODS = frozenset({"otp", "hwk", "webauthn"})


def mfa_satisfied(payload: dict, now_epoch: float) -> bool:
    """Mirrors ``CinxMfaVerifier``: step-up ACR, an approved method, and a recent sign-in."""
    if payload.get("acr") != REQUIRED_ACR:
        return False
    try:
        if not string_set_claim(payload, "amr") & APPROVED_MFA_METHODS:
            return False
        auth_time = float(payload.get("auth_time"))
    except (AuthorizationClaimError, TypeError, ValueError):
        return False
    return now_epoch - MFA_MAX_AGE_SECONDS <= auth_time <= now_epoch


def string_set_claim(payload: dict, name: str) -> frozenset[str]:
    raw = payload.get(name) or []
    if isinstance(raw, str):
        raw = [raw]
    if not isinstance(raw, (list, tuple, set)):
        raise AuthorizationClaimError(f"Invalid {name} claim")
    return frozenset(value.strip() for value in raw if isinstance(value, str) and value.strip())
