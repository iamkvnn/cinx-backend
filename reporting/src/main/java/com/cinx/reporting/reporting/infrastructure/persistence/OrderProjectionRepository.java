package com.cinx.reporting.reporting.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

public interface OrderProjectionRepository extends JpaRepository<OrderProjection, String> {
    long countByStatus(String status);

    @Query("select coalesce(sum(o.totalPrice - o.discounted), 0) from OrderProjection o where o.status = 'PAID'")
    BigDecimal paidRevenue();
}
