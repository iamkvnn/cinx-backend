package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.application.query.EnrollmentView;

import java.util.List;

public interface IEnrollmentService {
    /** The courses the learner is entitled to, the most recently granted first. */
    List<EnrollmentView> findMine(String userId);
}
