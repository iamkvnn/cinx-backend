package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface QuizOptionJpaRepository extends JpaRepository<QuizOptionJpaEntity, String> {
    void deleteAllByQuizQuestionId(String quizQuestionId);
}

