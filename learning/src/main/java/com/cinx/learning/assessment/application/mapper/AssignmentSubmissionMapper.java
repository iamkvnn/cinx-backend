package com.cinx.learning.assessment.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.assessment.application.query.AssignmentSubmissionView;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssignmentSubmissionMapper extends
        BaseMapper<AssignmentSubmission, AssignmentSubmissionView> {
}
