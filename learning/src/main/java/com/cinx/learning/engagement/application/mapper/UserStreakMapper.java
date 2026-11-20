package com.cinx.learning.engagement.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.engagement.application.query.UserStreakView;
import com.cinx.learning.engagement.domain.UserStreak;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserStreakMapper extends BaseMapper<UserStreak, UserStreakView> {
    // the lost count depends on today, so StreakService fills it
    @Override
    @Mapping(target = "lostStreak", ignore = true)
    UserStreakView toDto(UserStreak model);
}
