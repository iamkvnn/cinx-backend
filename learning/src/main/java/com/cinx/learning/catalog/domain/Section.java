package com.cinx.learning.catalog.domain;

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
public class Section extends AuditedModel {
    private String stableId;
    private String title;
    private String description;
    private Long duration;
    private Integer orderIndex;
    private String courseId;
    private String draftId;
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();
}
