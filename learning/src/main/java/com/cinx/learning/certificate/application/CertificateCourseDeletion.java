package com.cinx.learning.certificate.application;

import com.cinx.learning.certificate.domain.CourseCertificateConditionsRepository;
import com.cinx.learning.shared.application.CourseDeletionHook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
class CertificateCourseDeletion implements CourseDeletionHook {
    private final CourseCertificateConditionsRepository conditionsRepository;

    @Override
    public void onCourseDeleted(String courseId, Collection<String> lessonStableIds) {
        conditionsRepository.findByCourseId(courseId).ifPresent(conditionsRepository::delete);
    }
}
