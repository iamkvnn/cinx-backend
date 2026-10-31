package com.cinx.learning.learningpath.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import com.cinx.learning.learningpath.domain.UserLearningPath;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LearningPathMapper extends BaseMapper<UserLearningPath, LearningPathView> {
}
