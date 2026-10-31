package com.cinx.learning.enrollment.api.mapper;

import com.cinx.learning.enrollment.api.dto.response.EnrollmentResponse;
import com.cinx.learning.enrollment.api.dto.response.MyCourseResponse;
import com.cinx.learning.enrollment.application.query.EnrollmentView;
import com.cinx.learning.enrollment.application.query.MyCourseView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface EnrollmentApiMapper {
    com.cinx.learning.enrollment.api.dto.response.CheckEnrollmentStatus toResponse(com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus view);
    EnrollmentResponse toResponse(EnrollmentView view);
    MyCourseResponse toResponse(MyCourseView view);
    com.cinx.learning.enrollment.api.dto.response.MyCourseResponse.Category toResponse(com.cinx.learning.enrollment.application.query.MyCourseView.Category view);
    com.cinx.learning.enrollment.api.dto.response.MyCourseResponse.Instructor toResponse(com.cinx.learning.enrollment.application.query.MyCourseView.Instructor view);
    com.cinx.learning.enrollment.api.dto.response.MyCourseResponse.Next toResponse(com.cinx.learning.enrollment.application.query.MyCourseView.Next view);
    com.cinx.learning.enrollment.api.dto.response.MyCourseResponse.Certificate toResponse(com.cinx.learning.enrollment.application.query.MyCourseView.Certificate view);
}
