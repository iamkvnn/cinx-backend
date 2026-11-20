package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_offer")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanOfferJpaEntity {
    @Id private String id;
    @Column(name = "plan_version_id", nullable = false) private String planVersionId;
    @Column(name = "plan_id", nullable = false) private String planId;
    @Column(name = "price_id") private String priceId;
    // the table declares CHAR(2); without the JDBC type Hibernate expects VARCHAR and schema validation fails
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "market_code", nullable = false, length = 2) private String marketCode;
    private LocalDateTime salesStart;
    private LocalDateTime salesEnd;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private String status;
}
