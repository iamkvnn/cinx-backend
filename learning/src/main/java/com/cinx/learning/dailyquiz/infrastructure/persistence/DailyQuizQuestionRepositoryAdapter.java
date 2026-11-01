package com.cinx.learning.dailyquiz.infrastructure.persistence;

import com.cinx.learning.dailyquiz.domain.DailyQuizQuestion;
import com.cinx.learning.dailyquiz.domain.DailyQuizQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DailyQuizQuestionRepositoryAdapter implements DailyQuizQuestionRepository {
    private final DailyQuizQuestionJpaRepository jpa;
    private final DailyQuizQuestionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DailyQuizQuestion> findByActiveTrueOrderBySortOrderAscIdAsc() {
        return jpa.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyQuizQuestion> findAllByOrderBySortOrderAsc() {
        return jpa.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public int maxSortOrder() {
        return jpa.maxSortOrder();
    }

    @Override
    @Transactional
    public void delete(DailyQuizQuestion entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyQuizQuestion> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DailyQuizQuestion> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public DailyQuizQuestion save(DailyQuizQuestion entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<DailyQuizQuestion> saveAll(Iterable<DailyQuizQuestion> entities) {
        List<DailyQuizQuestion> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new DailyQuizQuestion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private DailyQuizQuestion store(DailyQuizQuestion entity) {
        DailyQuizQuestionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
