import unittest
from datetime import datetime
from unittest import mock

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.agent import auth
from app.agent.services import course_search
from app.agent.services.course_search import CourseSearchService
from app.agent.services.learner_context import LearnerContext
from app.api.recommendation import router
from app.core.database import Base, get_db
from app.core.problem import ProblemDetailException, problem_detail_exception_handler, validation_exception_handler
from app.entities.course import Course
from app.repositories.course_repository import CourseFilters


def _course(course_id, title, **fields):
    defaults = dict(status="PUBLISHED", price=100.0, discounted_price=None, rating=4.0, enrollment_count=10,
                    has_certificate=False, created_at=datetime(2026, 1, 1))
    defaults.update(fields)
    return Course(id=course_id, title=title, **defaults)


class SearchTestBase(unittest.TestCase):
    """Hybrid search over SQLite, with the dense and BM25 indexes replaced by fixed rankings."""

    def setUp(self):
        self.engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
        Base.metadata.create_all(self.engine, tables=[Course.__table__])
        self.db = Session(self.engine)
        self.db.add_all([
            _course("c1", "Python co ban", rating=4.8, enrollment_count=5, created_at=datetime(2026, 3, 1)),
            _course("c2", "Python nang cao", price=300.0, discounted_price=200.0, has_certificate=True, rating=4.1,
                    enrollment_count=50, created_at=datetime(2026, 5, 1)),
            _course("c3", "Python web", price=50.0, rating=3.5, enrollment_count=20, created_at=datetime(2026, 2, 1)),
            _course("c4", "Python cu", status="ARCHIVED"),
        ])
        self.db.commit()
        semantic = mock.patch.object(CourseSearchService, "_semantic_course_scores",
                                     return_value={"c1": 0.9, "c2": 0.8, "c4": 0.7})
        lexical = mock.patch.object(course_search.lexical_course_index, "search", return_value=[("c1", 3.0), ("c3", 2.0)])
        semantic.start()
        lexical.start()
        self.addCleanup(semantic.stop)
        self.addCleanup(lexical.stop)

    def tearDown(self):
        self.db.close()

    def search(self, top_k=24, filters=None, sort="relevance"):
        results, _, _, matched = CourseSearchService(self.db).search("python", top_k, filters or CourseFilters(), sort)
        return results, matched


class HybridSearchTest(SearchTestBase):
    def test_results_are_ranked_and_explained(self):
        results, matched = self.search()

        self.assertTrue(matched)
        self.assertEqual([r["courseId"] for r in results], ["c1", "c2", "c3"])
        self.assertEqual([r["rank"] for r in results], [1, 2, 3])
        self.assertEqual({r["reasonCode"] for r in results}, {"MATCHES_QUERY"})
        self.assertEqual([r["scoreBand"] for r in results], ["HIGH", "MEDIUM", "MEDIUM"])
        self.assertEqual({r["rankerVersion"] for r in results}, {"hybrid-search-v1"})

    def test_new_filters_exclude_what_they_should(self):
        on_sale, _ = self.search(filters=CourseFilters(on_sale=True))
        certificate, _ = self.search(filters=CourseFilters(has_certificate=True))
        priced, _ = self.search(filters=CourseFilters(price_from=60))

        self.assertEqual([r["courseId"] for r in on_sale], ["c2"])
        self.assertEqual([r["courseId"] for r in certificate], ["c2"])
        self.assertEqual([r["courseId"] for r in priced], ["c1", "c2"])

    def test_sorts_reorder_without_widening(self):
        orders = {sort: [r["courseId"] for r in self.search(sort=sort)[0]]
                  for sort in ("rating", "popular", "priceAsc", "priceDesc", "newest")}

        self.assertEqual(orders["rating"], ["c1", "c2", "c3"])
        self.assertEqual(orders["popular"], ["c2", "c3", "c1"])
        self.assertEqual(orders["priceAsc"], ["c3", "c1", "c2"])
        self.assertEqual(orders["priceDesc"], ["c2", "c1", "c3"])
        self.assertEqual(orders["newest"], ["c2", "c1", "c3"])

    def test_the_cap_is_twenty_four_and_the_assistant_default_five(self):
        self.assertEqual(course_search.MAX_RESULTS, 24)
        self.assertEqual(len(self.search(top_k=1)[0]), 1)
        self.assertEqual(len(self.search(top_k=100)[0]), 3)


class SearchApiTest(SearchTestBase):
    def setUp(self):
        super().setUp()
        app = FastAPI()
        app.add_exception_handler(ProblemDetailException, problem_detail_exception_handler)
        app.add_exception_handler(RequestValidationError, validation_exception_handler)
        app.include_router(router)
        app.dependency_overrides[get_db] = lambda: self.db
        self.client = TestClient(app)

    def test_a_guest_searches_without_a_token_and_gets_no_access_flags(self):
        response = self.client.get("/api/v1/recommendations/search", params={"query": "python", "limit": 24})

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(len(body["results"]), 3)
        self.assertNotIn("access", body["results"][0])
        self.assertNotIn("inWishlist", body["results"][0])

    def test_a_token_that_is_there_must_be_valid(self):
        response = self.client.get("/api/v1/recommendations/search", params={"query": "python"},
                                   headers={"Authorization": "Bearer not-a-jwt"})

        self.assertEqual(response.status_code, 401)

    def test_a_signed_in_learner_keeps_the_access_flags(self):
        user = auth.RequestUser(user_id="u1", token="t", permissions=frozenset())
        with mock.patch.object(auth, "current_user", return_value=user), \
                mock.patch("app.api.recommendation.load_learner_context", return_value=LearnerContext()) as learner:
            response = self.client.get("/api/v1/recommendations/search", params={"query": "python"},
                                       headers={"Authorization": "Bearer valid"})

        self.assertEqual(response.status_code, 200)
        self.assertIn("access", response.json()["results"][0])
        learner.assert_called_once_with(self.db, "u1")

    def test_limit_and_sort_are_validated(self):
        # the service answers validation problems with 400, like every cinx API
        self.assertEqual(self.client.get("/api/v1/recommendations/search", params={"query": "python", "limit": 25}).status_code, 400)
        self.assertEqual(self.client.get("/api/v1/recommendations/search", params={"query": "python", "sort": "random"}).status_code, 400)
        default = self.client.get("/api/v1/recommendations/search", params={"query": "python"}).json()
        self.assertLessEqual(len(default["results"]), 5)

    def test_filters_and_sort_reach_the_search(self):
        response = self.client.get("/api/v1/recommendations/search", params={
            "query": "python", "onSale": "true", "hasCertificate": "true", "minPrice": 1, "sort": "priceDesc", "limit": 24})

        self.assertEqual([r["courseId"] for r in response.json()["results"]], ["c2"])


if __name__ == "__main__":
    unittest.main()
