package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_price")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanPriceJpaEntity {
    @Id private String id;
    @Column(name = "plan_id", nullable = false) private String planId;
    @Column(name = "provider_product_id") private String providerProductId;
    // the table declares CHAR(2) and CHAR(3); without the JDBC type Hibernate expects VARCHAR and schema validation fails
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "market_code", nullable = false, length = 2) private String marketCode;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(name = "billing_interval", nullable = false) private String billingInterval;
    @Column(name = "interval_count", nullable = false) private int intervalCount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    private String provider;
    private String providerAccountKey;
    private String providerPriceRef;
    private String providerLookupKey;
    @Column(name = "tax_behavior", nullable = false) private String taxBehavior;
    @Column(nullable = false) private String status;
}
