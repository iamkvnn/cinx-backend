package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentLesson {
    private String lessonId;
    private String description;
    @Builder.Default
    private List<AssignmentAttachment> attachments = new ArrayList<>();
}
