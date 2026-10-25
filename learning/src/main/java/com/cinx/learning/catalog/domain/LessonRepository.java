package com.cinx.learning.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where Lesson rows are stored. */
public interface LessonRepository {
    /** The readable (published or archived) lessons with these stable ids, with section and course fetched. */
    List<Lesson> findReadableByStableIdIn(java.util.Collection<String> stableIds);

    Optional<Lesson> findBySectionAndStableId(String sectionId, String stableId);

    List<Lesson> findByCourseAndStableId(String courseId, String stableId);

    List<Lesson> findReadableByCourseAndStableId(String courseId, String stableId);

    List<Lesson> findPublishedByCourse(String courseId);

    /** Ids of the PUBLISHED courses whose published curriculum holds this lesson (draft-only copies are left out). */
    List<String> findPublishedCourseIdsByStableId(String stableId);

    /** Every copy of this lesson, draft or published, with its course: names a lesson outside a request. */
    List<LessonPlacement> findPlacementsByStableId(String stableId);

    List<Lesson> findEnrolledReadableByCourse(String courseId);

    /** Readable lessons of several courses, sections and courses fetched, in curriculum order within each course. */
    List<Lesson> findEnrolledReadableByCourseIn(java.util.Collection<String> courseIds);

    List<Lesson> findByDraft(String draftId);

    List<Lesson> findBySectionIdsForUpdate(List<String> sectionIds);

    Optional<Lesson> findDraftLessonForUpdate(String draftId, String stableId);

    boolean isAccessibleByInstructor(String lessonId, String userId);

    List<Lesson> findDraftByDraft(String draftId);

    void delete(Lesson entity);

    void deleteAll(Iterable<Lesson> entities);

    Lesson save(Lesson entity);

    List<Lesson> saveAll(Iterable<Lesson> entities);
}
