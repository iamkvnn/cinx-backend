package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.UpdateSubtitleTrackCommand;
import com.cinx.learning.catalog.application.query.SubtitleTrackView;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface SubtitleTrackMapper extends
        BaseMapper<SubtitleTrack, SubtitleTrackView>,
        UpdateMapper<SubtitleTrack, UpdateSubtitleTrackCommand> {

    ObjectMapper JSON = new ObjectMapper();

    @Override
    @Mapping(target = "qualityReport", source = "qualityReport", qualifiedByName = "qualityReport")
    SubtitleTrackView toDto(SubtitleTrack model);

    /** The stored worker report as an object; a report that no longer parses is left out rather than failing the read. */
    @Named("qualityReport")
    default Map<String, Object> qualityReport(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "languageCode", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    @Mapping(target = "fileKey", ignore = true)
    @Mapping(target = "originalFileKey", ignore = true)
    @Mapping(target = "wordConfidenceFileKey", ignore = true)
    @Mapping(target = "wordConfidenceFileUrl", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "fileType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "format", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "videoLessonId", ignore = true)
    @Mapping(target = "qualityReport", ignore = true)
    @Mapping(target = "aiOriginalFileKey", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "editedCueShare", ignore = true)
    @org.mapstruct.BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(@MappingTarget SubtitleTrack entity, UpdateSubtitleTrackCommand dto);
}

