import unittest

class AuthorizationClaimValidationTest(unittest.TestCase):
    def test_rejects_wrong_kind_and_bad_issued_at(self):
        from app.agent.auth_claims import AuthorizationClaimError, validate_authorization_claims

        with self.assertRaises(AuthorizationClaimError):
            validate_authorization_claims({"token_kind": "service", "iat": 100})
        with self.assertRaises(AuthorizationClaimError):
            validate_authorization_claims({"token_kind": "user"})
        with self.assertRaises(AuthorizationClaimError):
            validate_authorization_claims({"token_kind": "user", "iat": "bad"})

    def test_accepts_user_token(self):
        from app.agent.auth_claims import validate_authorization_claims

        validate_authorization_claims({"token_kind": "user", "iat": 100})


if __name__ == "__main__":
    unittest.main()
