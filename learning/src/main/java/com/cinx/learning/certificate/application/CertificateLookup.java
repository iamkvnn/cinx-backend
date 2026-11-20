package com.cinx.learning.certificate.application;

import com.cinx.learning.certificate.application.query.CertificateRequestInfo;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** What the other features read of certificate requests. */
public interface CertificateLookup {
    List<CertificateRequestInfo> requestsOfUser(String userId);

    List<CertificateRequestInfo> requestsOfCourse(String courseId);

    List<CertificateRequestInfo> pendingRequestsOfCourse(String courseId);

    /** How many of the course's pending requests already meet its certificate conditions. */
    long eligiblePendingCount(String courseId);

    /** Per course of these, how many requests wait for approval and since when. */
    List<PendingGroup> pendingGroups(Collection<String> courseIds);

    @Value
    class PendingGroup {
        String courseId;
        long waiting;
        LocalDateTime oldest;
    }
}
