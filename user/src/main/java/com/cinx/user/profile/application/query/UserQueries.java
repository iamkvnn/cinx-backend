package com.cinx.user.profile.application.query;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.profile.domain.InstructorApplicationStatus;

import java.time.LocalDateTime;

/** Port for the reads that cover many profiles, so none of them is loaded as an aggregate. */
public interface UserQueries {
    /** Profiles whose name or email contains {@code query}; each non-null filter narrows the result further. */
    PageResult<UserView> findAll(String query, Boolean isInstructorVerified,
                                 InstructorApplicationStatus applicationStatus, PageQuery page);

    long countTotalUsers();

    /** Profiles created between the two moments, both ends included. */
    long countUsersBetween(LocalDateTime start, LocalDateTime end);
}
