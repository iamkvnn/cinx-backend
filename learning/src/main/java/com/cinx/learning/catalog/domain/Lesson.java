package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.LessonType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends AuditedModel {
    private String stableId;
    private String title;
    private Long duration;
    private Integer orderIndex;
    private LessonType lessonType;
    private Boolean isPreview;
    @Builder.Default
    private List<String> prerequisiteIds = new ArrayList<>();
    private String sectionId;
}
