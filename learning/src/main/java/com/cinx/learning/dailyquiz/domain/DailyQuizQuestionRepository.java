package com.cinx.learning.dailyquiz.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored DailyQuizQuestion rows. */
public interface DailyQuizQuestionRepository {
    List<DailyQuizQuestion> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<DailyQuizQuestion> findAllByOrderBySortOrderAsc();

    int maxSortOrder();

    void delete(DailyQuizQuestion entity);

    List<DailyQuizQuestion> findAll();

    Optional<DailyQuizQuestion> findById(String id);

    DailyQuizQuestion save(DailyQuizQuestion entity);

    List<DailyQuizQuestion> saveAll(Iterable<DailyQuizQuestion> entities);
}
