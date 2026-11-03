package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "sellable_course")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellableCourseJpaEntity {
    @Id private String courseId;
    @Column(nullable = false) private String title;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    private String categoryId;
    private String instructorId;
    @Column(nullable = false) private boolean sellable;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
