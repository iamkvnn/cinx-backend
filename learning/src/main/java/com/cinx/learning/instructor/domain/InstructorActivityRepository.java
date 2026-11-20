package com.cinx.learning.instructor.domain;

import com.cinx.learning.shared.domain.InboxKind;

import com.cinx.common.paging.PageQuery;
import java.time.LocalDateTime;
import java.util.List;

/** Port for the stored InstructorActivity rows. */
public interface InstructorActivityRepository {
    List<InstructorActivity> findAllByCourseId(String courseId);

    List<InstructorActivity> findRecent(String instructorId, LocalDateTime since, InboxKind kind, String courseId, PageQuery pageable);

    long sumItemsSince(String instructorId, LocalDateTime since);

    void deleteAll(Iterable<InstructorActivity> entities);

    InstructorActivity save(InstructorActivity entity);
}
