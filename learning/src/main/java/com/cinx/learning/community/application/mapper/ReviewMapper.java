package com.cinx.learning.community.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.community.application.query.ReviewView;
import com.cinx.learning.community.domain.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper extends BaseMapper<Review, ReviewView> {
    @Override
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reply", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    ReviewView toDto(Review review);
}
