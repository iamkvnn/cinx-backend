import unittest
from datetime import datetime
from types import SimpleNamespace

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.core.database import Base
from app.entities.user_learning_profile import UserLearningProfile
from app.messaging.routing_keys import LEARNING_PROFILE_UPDATED
from app.models.events import UserLearningProfileEvent
from app.repositories.user_repository import UserRepository
from app.services.hybrid import RecommendationService


class LearningProfileProjectionTest(unittest.TestCase):
    def setUp(self):
        engine = create_engine("sqlite://")
        Base.metadata.create_all(engine, tables=[UserLearningProfile.__table__])
        self.db = Session(engine)
        self.repo = UserRepository(self.db)

    def tearDown(self):
        self.db.close()

    def test_the_newest_snapshot_wins_and_an_older_one_is_stale(self):
        self.assertTrue(self.repo.upsert_learning_profile("u1", "SKILL_UP", True, datetime(2026, 10, 13, 9, 0)))
        self.db.commit()
        self.assertFalse(self.repo.upsert_learning_profile("u1", "PERSONAL", False, datetime(2026, 10, 13, 8, 0)))
        self.assertTrue(self.repo.upsert_learning_profile("u1", None, False, datetime(2026, 10, 13, 10, 0)))
        self.db.commit()

        stored = self.repo.get_learning_profile("u1")
        self.assertEqual((stored.goal, stored.personalization_consent), (None, False))

    def test_the_event_keeps_only_what_recommendation_uses(self):
        event = UserLearningProfileEvent(**{"userId": "u1", "goal": "CERTIFICATE", "level": "BASIC", "minutesPerDay": 30,
                                            "contentLanguage": "VI", "categoryIds": ["c1"], "personalizationConsent": True,
                                            "occurredAt": "2026-10-13T09:00:00Z"})
        self.assertEqual((event.goal, event.personalizationConsent), ("CERTIFICATE", True))

    def test_the_routing_key_is_bound(self):
        from app.core.config import settings

        self.assertIn(LEARNING_PROFILE_UPDATED, settings.USER_ROUTING_KEYS.split(","))


class FakeUsers:
    def __init__(self, profile=None, categories=("c1",)):
        self.profile = profile
        self.categories = list(categories)

    def get_learning_profile(self, user_id):
        return self.profile

    def get_user_categories(self, user_id):
        return self.categories


def course(course_id, category_id, rating, enrollment_count, **fields):
    card = dict(title=f"Course {course_id}", category_name=None, instructor_id="i1", instructor_name="Instructor",
                image_urls=["https://img/1.png"], price=500000.0, discounted_price=None, has_certificate=True,
                is_in_subscription=False)
    card.update(fields)
    return SimpleNamespace(id=course_id, category_id=category_id, rating=rating, enrollment_count=enrollment_count, **card)


class FakeCourses:
    def get_published_courses_by_categoryId(self, category_ids):
        return [course("k1", "c1", 4.5, 200, title="Python thực chiến", price=499000.0)]

    def get_popular_courses(self, *args, **kwargs):
        return [course("pop", "c9", 4.0, 900)]

    def get_by_ids(self, ids):
        return [course(i, "c1", 4.0, 1) for i in ids]


class FakeInteractions:
    def get_user_interactions(self, user_id):
        return []


class FakeDb:
    def add(self, row):
        pass

    def commit(self):
        pass


def service(profile):
    svc = RecommendationService.__new__(RecommendationService)
    svc.db = FakeDb()
    svc.user_repo = FakeUsers(profile)
    svc.course_repo = FakeCourses()
    svc.interaction_repo = FakeInteractions()
    return svc


class OnboardingPicksTest(unittest.TestCase):
    def test_a_pick_from_the_chosen_topics_carries_the_goal(self):
        profile = SimpleNamespace(goal="CAREER_START", personalization_consent=True)
        items = service(profile).recommend_for_user("u1", 5)["recommendations"]

        self.assertEqual(items[0]["courseId"], "k1")
        self.assertEqual((items[0]["source"], items[0]["goal"]), ("ONBOARDING", "CAREER_START"))

    def test_each_pick_carries_its_card_so_a_paid_course_is_not_lost(self):
        items = service(None).recommend_for_user("u1", 5)["recommendations"]

        card = items[0]["course"]
        self.assertEqual((card["title"], card["price"], card["imageUrls"]), ("Python thực chiến", 499000.0, ["https://img/1.png"]))
        self.assertEqual((card["instructorName"], card["hasCertificate"]), ("Instructor", True))

    def test_without_a_profile_topics_still_drive_the_cold_start(self):
        items = service(None).recommend_for_user("u1", 5)["recommendations"]

        self.assertEqual(items[0]["source"], "ONBOARDING")
        self.assertNotIn("goal", items[0])

    def test_consent_switched_off_drops_the_topic_picks(self):
        profile = SimpleNamespace(goal="CAREER_START", personalization_consent=False)
        svc = service(profile)
        svc._content_based_recommendation = lambda *args: []
        svc._fallback_popular = lambda top_k, excluded: []

        result = svc.recommend_for_user("u1", 5)

        self.assertEqual(result["recommendations"], [])


if __name__ == "__main__":
    unittest.main()
