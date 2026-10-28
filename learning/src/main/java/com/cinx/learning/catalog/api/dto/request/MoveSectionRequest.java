package com.cinx.learning.catalog.api.dto.request;

public record MoveSectionRequest(
        String previousSectionId,
        String nextSectionId
) {
}

