package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.application.query.InstructorRevenueQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class InstructorRevenueQueriesAdapter implements InstructorRevenueQueries {
    private final InstructorRevenueJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public List<RevenueLine> findPaidLines(String instructorId, LocalDateTime start, LocalDateTime end, String courseId) {
        return jpa.findPaidLines(instructorId, start, end, courseId).stream()
                .map(line -> new RevenueLine(line.getCourseId(), line.getTitle(), line.getPriceSnapshot(),
                        line.getAllocatedAmount(), line.getFeePercent(), line.getPaidAt(), line.getOrderId()))
                .toList();
    }
}
