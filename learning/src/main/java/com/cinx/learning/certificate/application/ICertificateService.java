package com.cinx.learning.certificate.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.shared.domain.CertificateStatus;
import com.cinx.learning.certificate.application.command.CertificateConditionsCommand;
import com.cinx.learning.certificate.application.query.CertificateConditionsView;
import com.cinx.learning.certificate.application.query.CertificateRequestView;

import java.util.List;

public interface ICertificateService {
    CertificateRequestView applyForCertificate(String userId, String courseId);
    PageResult<CertificateRequestView> getRequestsByCourse(String currentUserId, String courseId, CertificateStatus status, int page, int size, String query, String sort);
    PageResult<CertificateRequestView> getAllRequests(String currentUserId, CertificateStatus status, int page, int size, String query, String sort);
    CertificateRequestView approveCertificate(String currentUserId, String requestId);
    CertificateConditionsView.BatchResult approveCertificates(String currentUserId, List<String> requestIds);
    CertificateRequestView rejectCertificate(String currentUserId, String requestId, String reason);
    CertificateRequestView getCertificate(String userId, String courseId);
    List<CertificateRequestView> getMyCertificates(String userId);
    CertificateConditionsView getConditions(String currentUserId, String courseId);
    CertificateConditionsView.Saved saveConditions(String currentUserId, String courseId, CertificateConditionsCommand request);
}
