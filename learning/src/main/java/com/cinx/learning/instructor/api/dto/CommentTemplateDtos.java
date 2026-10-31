package com.cinx.learning.instructor.api.dto;

import com.cinx.learning.instructor.domain.CommentTemplateKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentTemplateDtos {
    private CommentTemplateDtos() {
    }

    public record CreateRequest(@NotNull CommentTemplateKind kind, @NotBlank @Size(max = 2000) String content) {
    }

    public record Response(String id, CommentTemplateKind kind, String content, Instant createdAt) {
    }
}
