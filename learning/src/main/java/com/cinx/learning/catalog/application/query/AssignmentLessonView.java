package com.cinx.learning.catalog.application.query;

import java.util.List;

public record AssignmentLessonView (
        String description,
        List<AssignmentAttachmentView> attachments
){
}

