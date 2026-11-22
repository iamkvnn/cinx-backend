import unittest


NOW = 1_800_000_000.0


def mfa_payload(**overrides):
    payload = {"acr": "urn:cinx:loa:2", "amr": ["pwd", "otp"], "auth_time": NOW - 60}
    payload.update(overrides)
    return payload


class MfaSatisfiedTest(unittest.TestCase):
    """Same rules as CinxMfaVerifier: ACR urn:cinx:loa:2, an otp/hwk/webauthn method, sign-in within 900 s."""

    def test_accepts_step_up_token_signed_in_recently(self):
        from app.security.claims import mfa_satisfied

        self.assertTrue(mfa_satisfied(mfa_payload(), NOW))

    def test_rejects_wrong_acr(self):
        from app.security.claims import mfa_satisfied

        self.assertFalse(mfa_satisfied(mfa_payload(acr="1"), NOW))

    def test_rejects_password_only_methods(self):
        from app.security.claims import mfa_satisfied

        self.assertFalse(mfa_satisfied(mfa_payload(amr=["pwd"]), NOW))
        # "mfa" is not in the Java verifier's approved set either.
        self.assertFalse(mfa_satisfied(mfa_payload(amr=["mfa"]), NOW))

    def test_sign_in_age_boundary_is_900_seconds_inclusive(self):
        from app.security.claims import mfa_satisfied

        # NOW - 900 is exactly MAX_AGE old: still accepted, as !authTime.isBefore(now - 15 min) in Java.
        self.assertTrue(mfa_satisfied(mfa_payload(auth_time=NOW - 900), NOW))
        self.assertFalse(mfa_satisfied(mfa_payload(auth_time=NOW - 901), NOW))

    def test_rejects_sign_in_in_the_future_or_missing(self):
        from app.security.claims import mfa_satisfied

        self.assertFalse(mfa_satisfied(mfa_payload(auth_time=NOW + 1), NOW))
        payload = mfa_payload()
        del payload["auth_time"]
        self.assertFalse(mfa_satisfied(payload, NOW))

    def test_single_string_amr_counts(self):
        from app.security.claims import mfa_satisfied

        self.assertTrue(mfa_satisfied(mfa_payload(amr="webauthn"), NOW))


class AuthorizationClaimsTest(unittest.TestCase):
    def test_rejects_service_token_and_bad_issued_at(self):
        from app.security.claims import AuthorizationClaimError, validate_authorization_claims

        for payload in (
            {"token_kind": "service", "iat": NOW},
            {"token_kind": "user"},
            {"token_kind": "user", "iat": "x"},
        ):
            with self.subTest(payload=payload), self.assertRaises(AuthorizationClaimError):
                validate_authorization_claims(payload)

    def test_user_from_payload_uses_sub_and_reads_roles(self):
        from app.security.auth import user_from_payload

        caller = user_from_payload("t", {"sub": "s1", "roles": ["LEARNER"], "permissions": ["a", " ", 3]})
        self.assertEqual(caller.user_id, "s1")
        self.assertEqual(caller.roles, frozenset({"LEARNER"}))
        self.assertEqual(caller.permissions, frozenset({"a"}))
        self.assertFalse(caller.mfa)


if __name__ == "__main__":
    unittest.main()
