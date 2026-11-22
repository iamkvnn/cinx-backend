"""In-process BM25 keyword index over published courses.

Text is lowercased and Vietnamese diacritics are folded (``đ`` -> ``d``), so
"lap trinh" matches "lập trình".  Adjacent-syllable bigrams are indexed as
well, because Vietnamese words are often two syllables.
"""

import logging
import re
import threading
import unicodedata

import numpy as np
from rank_bm25 import BM25Okapi
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.entities.course import Course


logger = logging.getLogger(__name__)

_TOKEN = re.compile(r"[a-z0-9]+")


def fold(text: str | None) -> str:
    lowered = str(text or "").lower().replace("đ", "d")
    decomposed = unicodedata.normalize("NFD", lowered)
    return "".join(char for char in decomposed if unicodedata.category(char) != "Mn")


def tokenize(text: str | None) -> list[str]:
    words = _TOKEN.findall(fold(text))
    return words + [f"{first}_{second}" for first, second in zip(words, words[1:])]


def course_document(course: Course) -> str:
    parts = [course.title, course.title, course.category_name, course.instructor_name, course.description]
    for section in course.curriculum or []:
        if not isinstance(section, dict):
            continue
        parts.append(section.get("title"))
        for lesson in section.get("lessons", []) or []:
            if isinstance(lesson, dict):
                parts.append(lesson.get("title"))
    return " ".join(str(part) for part in parts if part)


class LexicalCourseIndex:
    def __init__(self):
        self._lock = threading.RLock()
        self._bm25: BM25Okapi | None = None
        self._course_ids: list[str] = []

    def rebuild(self, db: Session) -> None:
        courses = db.execute(select(Course).where(Course.status == "PUBLISHED")).scalars().all()
        course_ids: list[str] = []
        corpus: list[list[str]] = []
        for course in courses:
            tokens = tokenize(course_document(course))
            if tokens:
                course_ids.append(course.id)
                corpus.append(tokens)
        bm25 = BM25Okapi(corpus) if corpus else None
        with self._lock:
            self._bm25 = bm25
            self._course_ids = course_ids
        logger.info("Lexical course index rebuilt course_count=%s", len(course_ids))

    def search(self, query: str, top_k: int) -> list[tuple[str, float]]:
        tokens = tokenize(query)
        with self._lock:
            bm25, course_ids = self._bm25, self._course_ids
        if bm25 is None or not tokens:
            return []
        scores = bm25.get_scores(tokens)
        ranked = np.argsort(scores)[::-1][:top_k]
        return [(course_ids[index], float(scores[index])) for index in ranked if scores[index] > 0]


lexical_course_index = LexicalCourseIndex()
