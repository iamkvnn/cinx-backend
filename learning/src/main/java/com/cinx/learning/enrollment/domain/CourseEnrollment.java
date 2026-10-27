package com.cinx.learning.enrollment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollment {
    private String id;
    private String userId;
    private String courseId;
    private LocalDateTime enrolledAt;
    private String accessSourceAtEnrollment;
    private String status;
}
