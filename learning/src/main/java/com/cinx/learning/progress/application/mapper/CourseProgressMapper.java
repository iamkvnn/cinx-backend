package com.cinx.learning.progress.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.progress.application.command.UpdateCourseProgressCommand;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.domain.CourseProgress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseProgressMapper extends
        BaseMapper<CourseProgress, CourseProgressView>,
        UpdateMapper<CourseProgress, UpdateCourseProgressCommand> {
}
