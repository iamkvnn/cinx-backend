package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.community.domain.Report;
import com.cinx.learning.community.domain.ReportRepository;
import com.cinx.learning.community.domain.ReportType;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ReportRepositoryAdapter implements ReportRepository {
    private final ReportJpaRepository jpa;
    private final ReportPersistenceMapper mapper;

    @Override
    @Transactional
    public void deleteByRefIdAndType(String refId, ReportType type) {
        jpa.deleteByRefIdAndType(refId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Report> search(ReportType type, String query, PageQuery pageable) {
        return Pages.result(jpa.search(type, query, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countReportsBetween(LocalDateTime start, LocalDateTime end) {
        return jpa.countReportsBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countReportsByTypeBetween(LocalDateTime start, LocalDateTime end) {
        return jpa.countReportsByTypeBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateReportsByDay(LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateReportsByDay(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateReportsByMonth(LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateReportsByMonth(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findTopReportedRefs(LocalDateTime start, LocalDateTime end, PageQuery pageable) {
        return jpa.findTopReportedRefs(start, end, Pages.pageable(pageable));
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Report> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Report save(Report entity) {
        return store(entity);
    }

    /** Inserts a new Report, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Report store(Report entity) {
        ReportJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
