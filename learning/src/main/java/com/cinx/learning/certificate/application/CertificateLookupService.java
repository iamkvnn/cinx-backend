package com.cinx.learning.certificate.application;

import com.cinx.learning.certificate.application.query.CertificateRequestInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.certificate.domain.CertificateRequestRepository;
import com.cinx.learning.certificate.domain.CourseCertificateConditions;
import com.cinx.learning.shared.domain.CertificateStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateLookupService implements CertificateLookup {
    private final CertificateRequestRepository requestRepository;
    private final CertificateEligibilityService eligibilityService;

    @Override
    public List<CertificateRequestInfo> requestsOfUser(String userId) {
        return requestRepository.findByUserId(userId).stream().map(CertificateRequestInfo::of).toList();
    }

    @Override
    public List<CertificateRequestInfo> requestsOfCourse(String courseId) {
        return requestRepository.findAllByCourseId(courseId).stream().map(CertificateRequestInfo::of).toList();
    }

    @Override
    public List<CertificateRequestInfo> pendingRequestsOfCourse(String courseId) {
        return requestRepository.findAllByCourseIdAndStatus(courseId, CertificateStatus.PENDING).stream()
                .map(CertificateRequestInfo::of)
                .toList();
    }

    @Override
    public long eligiblePendingCount(String courseId) {
        List<CertificateRequestInfo> pending = pendingRequestsOfCourse(courseId);
        if (pending.isEmpty()) {
            return 0;
        }
        CourseCertificateConditions conditions = eligibilityService.conditionsOf(courseId);
        List<LessonInfo> lessons = eligibilityService.lessonsOf(courseId);
        return pending.stream()
                .filter(request -> eligibilityService.evaluate(request.getUserId(), courseId, conditions, lessons).eligible())
                .count();
    }

    @Override
    public List<PendingGroup> pendingGroups(Collection<String> courseIds) {
        return requestRepository.findGroups(courseIds, CertificateStatus.PENDING).stream()
                .map(row -> new PendingGroup(row.getCourseId(), row.getWaiting(), row.getOldest()))
                .toList();
    }
}
