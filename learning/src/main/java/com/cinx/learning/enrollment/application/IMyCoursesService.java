package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.application.query.MyCourseView;

import java.util.List;

public interface IMyCoursesService {
    /** Every course the user holds an entitlement for, most recently studied first. */
    List<MyCourseView> myCourses(String userId);
}
