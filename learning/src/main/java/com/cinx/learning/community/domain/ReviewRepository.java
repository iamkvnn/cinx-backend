package com.cinx.learning.community.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where Review rows are stored. */
public interface ReviewRepository {
    PageResult<Review> findByCourseId(String courseId, PageQuery pageable);

    /** Reviews of a course its instructors have not replied to yet. */
    PageResult<Review> findUnrepliedByCourseId(String courseId, PageQuery pageable);

    /** Unreplied reviews of these courses: count and the oldest one, per course. */
    List<UnrepliedGroup> findUnrepliedGroups(java.util.Collection<String> courseIds);

    Double averageRating(java.util.Collection<String> courseIds);

    Double averageRatingBefore(java.util.Collection<String> courseIds, java.time.LocalDateTime before);

    long countByCourseIdIn(java.util.Collection<String> courseIds);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    Long countByCourseId(String courseId);

    Double getAverageRatingByCourseId(String courseId);

    List<Object[]> countRatingsByCourseId(String courseId);

    void delete(Review entity);

    boolean existsById(String id);

    List<Review> findAllById(Iterable<String> ids);

    Optional<Review> findById(String id);

    Review save(Review entity);

    interface UnrepliedGroup {
            String getCourseId();
            long getWaiting();
            java.time.LocalDateTime getOldest();
        }
}
