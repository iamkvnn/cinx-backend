package com.cinx.commerce.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "commerce_order_item", uniqueConstraints = @UniqueConstraint(name = "uk_order_course", columnNames = {"order_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CommerceOrderItemJpaEntity {
    @Id
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CommerceOrderJpaEntity order;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(nullable = false)
    private String titleSnapshot;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;
    @Column(name = "instructor_id")
    private String instructorId;
    @Column(name = "allocated_amount", precision = 19, scale = 2)
    private BigDecimal allocatedAmount;
    @Column(name = "platform_fee_percent", precision = 5, scale = 2)
    private BigDecimal platformFeePercent;
}
