package com.cinx.reporting.reporting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "order_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderProjection {
    @Id
    private String orderId;
    private String userId;
    @Column(precision = 19, scale = 2)
    private BigDecimal totalPrice;
    @Column(precision = 19, scale = 2)
    private BigDecimal discounted;
    private String status;
    private LocalDateTime orderDate;
    private LocalDateTime updatedAt;
}
