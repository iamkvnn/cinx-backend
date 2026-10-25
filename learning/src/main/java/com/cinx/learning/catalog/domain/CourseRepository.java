package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.CoursePublishStatus;

import com.cinx.learning.shared.domain.CourseStatus;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Port: where Course rows are stored. */
public interface CourseRepository {
    PageResult<Course> searchAll(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, Boolean hasCertificate, Boolean onSale, CourseStatus status, CoursePublishStatus publishStatus, PageQuery pageable);

    /** {@code durationFrom} inclusive, {@code durationTo} exclusive, both in minutes; null leaves that side open. */
    PageResult<Course> searchPublished(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, Boolean hasCertificate, Boolean onSale, Long durationFrom, Long durationTo, PageQuery pageable);

    long countDistinctInstructorsOfPublishedCourses();

    List<Object[]> countPublishedByCategory();

    List<Course> findPublishedByIds(List<String> ids);

    List<Course> findEnrolledReadableByIds(List<String> ids);

    List<String> searchEnrolledReadableIds(List<String> ids, String query);

    long countByInstructorId(String instructorId);

    long countByInstructorIdAndStatus(String instructorId, CourseStatus status);

    List<String> findIdsByInstructorId(String instructorId);

    long countByStatus(CourseStatus status);

    Double averageRatingByInstructorId(String instructorId);

    Long sumEnrollmentCountByInstructorId(String instructorId);

    InstructorPublishedStats summarizePublishedByInstructor(String instructorId);

    /** Rows of {category id, category name, course count}, most courses first. */
    List<Object[]> countPublishedByCategoryForInstructor(String instructorId);

    long countCreatedCoursesBetween(LocalDateTime start, LocalDateTime end);

    long countCreatedCoursesByInstructorBetween(String instructorId, LocalDateTime start, LocalDateTime end);

    List<Object[]> countCreatedCoursesByStatusBetween(LocalDateTime start, LocalDateTime end);

    List<Object[]> countCurrentCoursesByStatus();

    List<Object[]> aggregateCreatedCoursesByDay(LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateCreatedCoursesByMonth(LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateCreatedCoursesByInstructorAndDay(String instructorId, LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateCreatedCoursesByInstructorAndMonth(String instructorId, LocalDateTime start, LocalDateTime end);

    void delete(Course entity);

    boolean existsById(String id);

    List<Course> findAll();

    List<Course> findAllById(Iterable<String> ids);

    Optional<Course> findById(String id);

    Course save(Course entity);

    /** Public figures of one instructor, over PUBLISHED courses they own or are an ACTIVE member of. */
    interface InstructorPublishedStats {
            Long getCourseCount();
    
            Long getEnrollmentCount();
    
            Long getRatingCount();
    
            /** Rating votes on courses that have a rating: the denominator of the weighted average. */
            Long getRatedCount();
    
            /** Sum of rating × ratingCount, so that dividing by {@link #getRatedCount()} weights each course by its votes. */
            Double getWeightedRatingSum();
        }
}
