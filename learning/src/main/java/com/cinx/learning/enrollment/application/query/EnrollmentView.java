package com.cinx.learning.enrollment.application.query;

import java.time.LocalDateTime;

public record EnrollmentView(String id, String courseId, String sourceOrderId, LocalDateTime grantedAt) {
}
