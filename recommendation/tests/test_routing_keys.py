import unittest

from app.messaging.routing_keys import (
    ENROLLMENT_GRANTED,
    WISHLIST_ADDED,
    WISHLIST_REMOVED,
    is_wishlist_event,
)


class RoutingKeyContractTest(unittest.TestCase):
    def test_enrollment_uses_learning_contract(self):
        self.assertEqual("learning.enrollment.granted", ENROLLMENT_GRANTED)

    def test_only_commerce_wishlist_events_are_accepted(self):
        self.assertTrue(is_wishlist_event(WISHLIST_ADDED))
        self.assertTrue(is_wishlist_event(WISHLIST_REMOVED))
        self.assertFalse(is_wishlist_event("social.wishlist.added"))


if __name__ == "__main__":
    unittest.main()
