"""Course search quality on a set of queries, against the live database and index.

Run from ``recommendation``:  python -m evals.search [--cases PATH] [--k 5]

Expectations name categories or title keywords rather than ids, so one case
file works across environments; edit them to match your catalog.  An expect
of ``{"empty": true}`` wants no topic match, ``{"any": true}`` any result.  The best
semantic score of each query is printed next to its outcome, which is what
AGENT_SEARCH_MIN_SCORE should be calibrated against.
"""

import argparse
import json
import statistics
import sys
from pathlib import Path

from app.agent.services.course_search import CourseSearchService, filters_from_arguments
from app.core.database import SessionLocal
from app.core.config import settings
from app.services.embedding_model import embed_query
from app.services.lexical_index import fold
from app.services.rag_index import rag_index, rebuild_rag_index


DEFAULT_CASES = Path(__file__).with_name("search_cases.json")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--k", type=int, default=5)
    args = parser.parse_args(argv)

    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    db = SessionLocal()
    try:
        rebuild_rag_index(db)
        service = CourseSearchService(db)
        rows = [_evaluate(service, case, args.k) for case in cases]
    finally:
        db.close()

    relevant = [row for row in rows if not row["expectEmpty"]]
    empty = [row for row in rows if row["expectEmpty"]]
    print(f"AGENT_SEARCH_MIN_SCORE = {settings.AGENT_SEARCH_MIN_SCORE}")
    if relevant:
        print(f"hit@{args.k}: {sum(row['ok'] for row in relevant)}/{len(relevant)}")
    if empty:
        print(f"no-match accuracy: {sum(row['ok'] for row in empty)}/{len(empty)}")
    for label, group in (("relevant", relevant), ("no-match", empty)):
        scores = [row["bestSemantic"] for row in group if row["bestSemantic"] is not None]
        if scores:
            print(
                f"best semantic score, {label} queries: min {min(scores):.3f} "
                f"median {statistics.median(scores):.3f} max {max(scores):.3f}"
            )
    print()
    for row in rows:
        mark = "ok  " if row["ok"] else "FAIL"
        best = f"{row['bestSemantic']:.3f}" if row["bestSemantic"] is not None else "  -  "
        print(f"{mark} best={best} {row['query']!r} -> {row['titles']}")
    return 0


def _evaluate(service: CourseSearchService, case: dict, k: int) -> dict:
    query = case["query"]
    expect = case.get("expect") or {}
    results, _citations, _filters, topic_matched = service.search(
        query, top_k=k, filters=filters_from_arguments(case.get("filters") or {})
    )
    semantic = rag_index.search(embed_query(query), 1, source_type="course")
    expect_empty = bool(expect.get("empty"))
    if expect_empty:
        ok = not results or not topic_matched
    elif expect.get("any"):
        # A filter-only request: any course meeting the filters will do.
        ok = bool(results)
    else:
        ok = topic_matched and any(_satisfies(result, expect) for result in results)
    return {
        "query": query,
        "expectEmpty": expect_empty,
        "ok": ok,
        "bestSemantic": semantic[0][1] if semantic else None,
        "titles": [result.get("title") for result in results],
    }


def _satisfies(result: dict, expect: dict) -> bool:
    category = fold(result.get("categoryName") or "")
    title = fold(result.get("title") or "")
    if any(fold(name) in category for name in expect.get("anyCategory") or []):
        return True
    return any(fold(keyword) in title for keyword in expect.get("titleKeywords") or [])


if __name__ == "__main__":
    sys.exit(main())
