package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface QuizScoreHistoryJpaRepository extends JpaRepository<QuizScoreHistoryJpaEntity, String> {
}
