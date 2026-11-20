package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CategoryRepositoryAdapter implements CategoryRepository {
    private final CategoryJpaRepository jpa;
    private final CategoryPersistenceMapper mapper;

    @Override
    @Transactional
    public void delete(Category entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Category save(Category entity) {
        return store(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Inserts a new Category, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Category store(Category entity) {
        CategoryJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
