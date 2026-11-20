package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseImage;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.infrastructure.persistence.ChildSync;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseRepositoryAdapter implements CourseRepository {
    private final CourseJpaRepository jpa;
    private final CoursePersistenceMapper mapper;
    private final CourseImagePersistenceMapper courseImageMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Course> searchAll(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, Boolean hasCertificate, Boolean onSale, CourseStatus status, CoursePublishStatus publishStatus, PageQuery pageable) {
        return Pages.result(jpa.searchAll(query, categoryId, instructorId, rating, priceFrom, priceTo, hasCertificate, onSale, status, publishStatus, Pages.pageable(pageable, "rating")), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Course> searchPublished(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, Boolean hasCertificate, Boolean onSale, Long durationFrom, Long durationTo, PageQuery pageable) {
        return Pages.result(jpa.searchPublished(query, categoryId, instructorId, rating, priceFrom, priceTo, hasCertificate, onSale, durationFrom, durationTo, Pages.pageable(pageable, "rating")), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctInstructorsOfPublishedCourses() {
        return jpa.countDistinctInstructorsOfPublishedCourses();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countPublishedByCategory() {
        return jpa.countPublishedByCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findPublishedByIds(List<String> ids) {
        return jpa.findPublishedByIds(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findEnrolledReadableByIds(List<String> ids) {
        return jpa.findEnrolledReadableByIds(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> searchEnrolledReadableIds(List<String> ids, String query) {
        return jpa.searchEnrolledReadableIds(ids, query);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInstructorId(String instructorId) {
        return jpa.countByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInstructorIdAndStatus(String instructorId, CourseStatus status) {
        return jpa.countByInstructorIdAndStatus(instructorId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findIdsByInstructorId(String instructorId) {
        return jpa.findIdsByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(CourseStatus status) {
        return jpa.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Double averageRatingByInstructorId(String instructorId) {
        return jpa.averageRatingByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long sumEnrollmentCountByInstructorId(String instructorId) {
        return jpa.sumEnrollmentCountByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorPublishedStats summarizePublishedByInstructor(String instructorId) {
        return wrapInstructorPublishedStats(jpa.summarizePublishedByInstructor(instructorId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countPublishedByCategoryForInstructor(String instructorId) {
        return jpa.countPublishedByCategoryForInstructor(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCreatedCoursesBetween(LocalDateTime start, LocalDateTime end) {
        return jpa.countCreatedCoursesBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCreatedCoursesByInstructorBetween(String instructorId, LocalDateTime start, LocalDateTime end) {
        return jpa.countCreatedCoursesByInstructorBetween(instructorId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countCreatedCoursesByStatusBetween(LocalDateTime start, LocalDateTime end) {
        return jpa.countCreatedCoursesByStatusBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countCurrentCoursesByStatus() {
        return jpa.countCurrentCoursesByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCreatedCoursesByDay(LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateCreatedCoursesByDay(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCreatedCoursesByMonth(LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateCreatedCoursesByMonth(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCreatedCoursesByInstructorAndDay(String instructorId, LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateCreatedCoursesByInstructorAndDay(instructorId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCreatedCoursesByInstructorAndMonth(String instructorId, LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateCreatedCoursesByInstructorAndMonth(instructorId, start, end);
    }

    @Override
    @Transactional
    public void delete(Course entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpa.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Course save(Course entity) {
        return store(entity);
    }

    /** Inserts a new Course, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Course store(Course entity) {
        CourseJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        ChildSync.sync(stored.getImages(), entity.getImages(), CourseImageJpaEntity::getId, CourseImage::getId, child -> {
            CourseImageJpaEntity row = courseImageMapper.toEntity(child);
            row.setCourse(stored);
            return row;
        });
        return mapper.toDomain(jpa.save(stored));
    }

    private static CourseRepository.InstructorPublishedStats wrapInstructorPublishedStats(CourseJpaRepository.InstructorPublishedStats row) {
        if (row == null) {
            return null;
        }
        return new CourseRepository.InstructorPublishedStats() {
            @Override
            public Long getCourseCount() {
                return row.getCourseCount();
            }
            @Override
            public Long getEnrollmentCount() {
                return row.getEnrollmentCount();
            }
            @Override
            public Long getRatingCount() {
                return row.getRatingCount();
            }
            @Override
            public Long getRatedCount() {
                return row.getRatedCount();
            }
            @Override
            public Double getWeightedRatingSum() {
                return row.getWeightedRatingSum();
            }
        };
    }
}
