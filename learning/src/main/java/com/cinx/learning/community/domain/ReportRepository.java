package com.cinx.learning.community.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Port: where Report rows are stored. */
public interface ReportRepository {
    void deleteByRefIdAndType(String refId, ReportType type);

    PageResult<Report> search(ReportType type, String query, PageQuery pageable);

    Long countReportsBetween(LocalDateTime start, LocalDateTime end);

    List<Object[]> countReportsByTypeBetween(LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateReportsByDay(LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateReportsByMonth(LocalDateTime start, LocalDateTime end);

    List<Object[]> findTopReportedRefs(LocalDateTime start, LocalDateTime end, PageQuery pageable);

    void deleteById(String id);

    Optional<Report> findById(String id);

    Report save(Report entity);
}
