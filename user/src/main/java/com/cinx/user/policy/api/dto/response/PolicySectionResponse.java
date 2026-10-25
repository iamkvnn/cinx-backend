package com.cinx.user.policy.api.dto.response;

public record PolicySectionResponse(
        String id,
        String heading,
        String anchor,
        String bodyMarkdown,
        Integer orderIndex
) {
}
