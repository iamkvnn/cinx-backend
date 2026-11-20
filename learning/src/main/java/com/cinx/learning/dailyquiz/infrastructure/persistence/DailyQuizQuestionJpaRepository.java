package com.cinx.learning.dailyquiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface DailyQuizQuestionJpaRepository extends JpaRepository<DailyQuizQuestionJpaEntity, String> {
    List<DailyQuizQuestionJpaEntity> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<DailyQuizQuestionJpaEntity> findAllByOrderBySortOrderAsc();

    @Query("SELECT COALESCE(MAX(q.sortOrder), -1) FROM DailyQuizQuestion q")
    int maxSortOrder();
}
