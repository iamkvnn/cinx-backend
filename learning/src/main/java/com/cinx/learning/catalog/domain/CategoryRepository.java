package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where Category rows are stored. */
public interface CategoryRepository {
    void delete(Category entity);

    List<Category> findAll();

    Optional<Category> findById(String id);

    Category save(Category entity);

    List<Category> findAllById(Iterable<String> ids);
}
