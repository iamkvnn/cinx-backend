package com.cinx.learning.catalog.application.query;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.AssignmentLesson;

/** What other features may read of a AssignmentLesson; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class AssignmentLessonInfo {
    String lessonId;
    String description;
    List<AssignmentAttachmentInfo> attachments;

    public static AssignmentLessonInfo of(AssignmentLesson domain) {
        return AssignmentLessonInfo.builder()
                .lessonId(domain.getLessonId())
                .description(domain.getDescription())
                .attachments(domain.getAttachments() == null ? null : domain.getAttachments().stream().map(AssignmentAttachmentInfo::of).toList())
                .build();
    }
}
