package com.cinx.commerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

interface InstructorRevenueJpaRepository extends Repository<CommerceOrderItemJpaEntity, String> {
    @Query("""
        select i.courseId as courseId, i.titleSnapshot as title, i.priceSnapshot as priceSnapshot,
               i.allocatedAmount as allocatedAmount, i.platformFeePercent as feePercent,
               o.paidAt as paidAt, o.id as orderId
        from CommerceOrderItemJpaEntity i join i.order o
        where i.instructorId = :instructorId
          and o.status = com.cinx.commerce.order.domain.OrderStatus.PAID
          and o.paidAt >= :start and o.paidAt < :end
          and (:courseId is null or i.courseId = :courseId)
    """)
    List<RevenueLine> findPaidLines(@Param("instructorId") String instructorId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("courseId") String courseId);

    interface RevenueLine {
        String getCourseId();

        String getTitle();

        BigDecimal getPriceSnapshot();

        BigDecimal getAllocatedAmount();

        BigDecimal getFeePercent();

        LocalDateTime getPaidAt();

        String getOrderId();
    }
}
