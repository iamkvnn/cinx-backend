import unittest
from datetime import datetime, timezone

from app.services.quota_contract import period_bounds, request_fingerprint, snapshot_hash


class EntitlementQuotaContractTest(unittest.TestCase):
    def test_fingerprint_changes_when_business_input_changes(self):
        first = request_fingerprint("u1", "s1", "hello", "debug")
        retry = request_fingerprint("u1", "s1", "hello", "debug")
        changed = request_fingerprint("u1", "s1", "different", "debug")
        self.assertEqual(first, retry)
        self.assertNotEqual(first, changed)

    def test_period_is_deterministic_and_utc(self):
        key, start, end = period_bounds("MONTH", datetime(2026, 8, 2, 12, tzinfo=timezone.utc))
        self.assertTrue(key.startswith("MONTH:2026-08-01"))
        self.assertEqual(start.day, 1)
        self.assertEqual(end.month, 9)

    def test_snapshot_hash_is_order_independent(self):
        self.assertEqual(snapshot_hash({"b": 2, "a": 1}), snapshot_hash({"a": 1, "b": 2}))


if __name__ == "__main__":
    unittest.main()
