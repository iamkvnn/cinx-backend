package com.cinx.learning.enrollment.api.dto.response;

import java.time.LocalDateTime;

public record EnrollmentResponse(String id, String courseId, String sourceOrderId, LocalDateTime grantedAt) {
}
