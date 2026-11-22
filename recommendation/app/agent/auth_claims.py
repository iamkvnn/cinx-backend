class AuthorizationClaimError(ValueError):
    pass


def validate_authorization_claims(payload: dict) -> None:
    if payload.get("token_kind") != "user":
        raise AuthorizationClaimError("Unexpected token kind")
    try:
        int(payload["iat"])
    except (KeyError, TypeError, ValueError) as exc:
        raise AuthorizationClaimError("Invalid issued-at") from exc
