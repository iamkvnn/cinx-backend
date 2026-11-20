package com.cinx.learning.studyplan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface StudyPlanJpaRepository extends JpaRepository<StudyPlanJpaEntity, String> {
    Optional<StudyPlanJpaEntity> findByUserId(String userId);

    @Query("SELECT p.userId FROM StudyPlan p")
    List<String> findAllUserIds();
}
