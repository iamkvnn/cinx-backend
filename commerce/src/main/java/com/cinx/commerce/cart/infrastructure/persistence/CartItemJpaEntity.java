package com.cinx.commerce.cart.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_item", uniqueConstraints = @UniqueConstraint(name = "uk_cart_user_course", columnNames = {"user_id", "course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemJpaEntity {
    @Id private String id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "course_id", nullable = false) private String courseId;
    @Column(nullable = false) private LocalDateTime addedAt;
}
