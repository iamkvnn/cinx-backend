package com.cinx.learning.instructor.application;

import com.cinx.learning.instructor.application.query.OverviewView;

import java.time.LocalDate;

public interface IInstructorOverviewService {
    OverviewView overview(String instructorId, LocalDate from, LocalDate to, String courseId);

    /** The per-course table of the overview as CSV. */
    String overviewCsv(String instructorId, LocalDate from, LocalDate to);
}
