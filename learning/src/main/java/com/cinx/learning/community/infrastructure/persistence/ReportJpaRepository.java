package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, String> {
    Page<ReportJpaEntity> findByType(ReportType type, Pageable pageable);
    void deleteByRefIdAndType(String refId, ReportType type);
    List<ReportJpaEntity> findAllByRefIdAndType(String refId, ReportType type);

    @Query("""
        SELECT r
        FROM Report r
        WHERE (:type IS NULL OR r.type = :type)
          AND (:query IS NULL OR r.id LIKE %:query% OR r.reporterId LIKE %:query% OR r.refId LIKE %:query% OR r.reason LIKE %:query%)
    """)
    Page<ReportJpaEntity> search(ReportType type, String query, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt BETWEEN :start AND :end")
    Long countReportsBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT r.type, COUNT(r)
        FROM Report r
        WHERE r.createdAt BETWEEN :start AND :end
        GROUP BY r.type
    """)
    List<Object[]> countReportsByTypeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m-%d'), COUNT(r)
        FROM Report r
        WHERE r.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m-%d') ASC
    """)
    List<Object[]> aggregateReportsByDay(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m'), COUNT(r)
        FROM Report r
        WHERE r.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m') ASC
    """)
    List<Object[]> aggregateReportsByMonth(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT r.refId, r.type, COUNT(r)
        FROM Report r
        WHERE r.createdAt BETWEEN :start AND :end
        GROUP BY r.refId, r.type
        ORDER BY COUNT(r) DESC
    """)
    List<Object[]> findTopReportedRefs(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
