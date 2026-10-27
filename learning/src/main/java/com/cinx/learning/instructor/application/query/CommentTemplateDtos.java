package com.cinx.learning.instructor.application.query;

import com.cinx.learning.instructor.domain.CommentTemplateKind;

import java.time.Instant;

public final class CommentTemplateDtos {
    private CommentTemplateDtos() {
    }

    public record CreateRequest( CommentTemplateKind kind,   String content) {
    }

    public record Response(String id, CommentTemplateKind kind, String content, Instant createdAt) {
    }
}
