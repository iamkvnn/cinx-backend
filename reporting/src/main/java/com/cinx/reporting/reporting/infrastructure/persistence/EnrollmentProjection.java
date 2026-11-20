package com.cinx.reporting.reporting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EnrollmentProjection {
    @Id
    private String id;
    private String userId;
    private String courseId;
    private LocalDateTime grantedAt;
}
