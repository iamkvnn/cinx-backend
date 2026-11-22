ENROLLMENT_GRANTED = "learning.enrollment.granted"
WISHLIST_ADDED = "commerce.wishlist.added"
WISHLIST_REMOVED = "commerce.wishlist.removed"
COMMERCE_ENTITLEMENTS_CHANGED = "commerce.subscription.entitlements.changed"
LEARNING_PROFILE_UPDATED = "user.learning-profile.updated"

WISHLIST_EVENTS = frozenset({WISHLIST_ADDED, WISHLIST_REMOVED})


def is_wishlist_event(routing_key: str) -> bool:
    return routing_key in WISHLIST_EVENTS
