package com.cinx.learning.certificate.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CourseCertificateConditionsJpaRepository extends JpaRepository<CourseCertificateConditionsJpaEntity, String> {
    Optional<CourseCertificateConditionsJpaEntity> findByCourseId(String courseId);
}
