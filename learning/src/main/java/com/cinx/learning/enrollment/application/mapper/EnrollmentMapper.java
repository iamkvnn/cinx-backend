package com.cinx.learning.enrollment.application.mapper;

import com.cinx.learning.enrollment.application.query.EnrollmentView;
import com.cinx.learning.enrollment.domain.CourseEntitlement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    EnrollmentView toResponse(CourseEntitlement entitlement);
}
