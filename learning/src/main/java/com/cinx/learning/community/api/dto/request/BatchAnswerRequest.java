package com.cinx.learning.community.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** One instructor answer posted to several questions ("Trả lời chung"): each question gets its own copy. */
public record BatchAnswerRequest(
        @NotEmpty @Size(max = 20) List<@NotBlank String> questionIds,
        @NotBlank @Size(max = 10000) String content,
        // true when the instructor started from the assistant's draft; the learner is told
        Boolean aiAssisted
) {
}
