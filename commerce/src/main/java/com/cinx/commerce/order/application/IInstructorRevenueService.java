package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.query.InstructorRevenue;

import java.time.LocalDate;

public interface IInstructorRevenueService {
    /** Revenue of the lines sold under this instructor, by business day (Asia/Ho_Chi_Minh), from and to inclusive. */
    InstructorRevenue revenue(String instructorId, LocalDate from, LocalDate to, String courseId);
}
