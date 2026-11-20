package com.cinx.learning.certificate.domain;

import java.util.Optional;

/** Port for the stored CourseCertificateConditions rows. */
public interface CourseCertificateConditionsRepository {
    Optional<CourseCertificateConditions> findByCourseId(String courseId);

    void delete(CourseCertificateConditions entity);

    CourseCertificateConditions saveAndFlush(CourseCertificateConditions entity);
}
