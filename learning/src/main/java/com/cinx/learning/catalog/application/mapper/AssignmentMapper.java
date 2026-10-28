package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.query.AssignmentLessonView;
import com.cinx.learning.catalog.domain.AssignmentLesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssignmentMapper extends BaseMapper<AssignmentLesson, AssignmentLessonView>,
        CreateMapper<AssignmentLesson, CreateAssignmentLessonCommand>,
        UpdateMapper<AssignmentLesson, UpdateAssignmentLessonCommand> {
}

