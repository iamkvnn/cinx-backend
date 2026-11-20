package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.catalog.application.query.SubtitleJobView;
import com.cinx.learning.catalog.domain.SubtitleJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubtitleJobMapper extends BaseMapper<SubtitleJob, SubtitleJobView> {
    @Override
    SubtitleJobView toDto(SubtitleJob model);
}

