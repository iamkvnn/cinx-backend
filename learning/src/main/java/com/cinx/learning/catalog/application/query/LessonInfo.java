package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.LessonType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.Lesson;

/** What other features may read of a Lesson; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class LessonInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String stableId;
    String title;
    Long duration;
    Integer orderIndex;
    LessonType lessonType;
    Boolean isPreview;
    List<String> prerequisiteIds;
    String sectionId;

    public static LessonInfo of(Lesson domain) {
        return LessonInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .stableId(domain.getStableId())
                .title(domain.getTitle())
                .duration(domain.getDuration())
                .orderIndex(domain.getOrderIndex())
                .lessonType(domain.getLessonType())
                .isPreview(domain.getIsPreview())
                .prerequisiteIds(domain.getPrerequisiteIds())
                .sectionId(domain.getSectionId())
                .build();
    }
}
