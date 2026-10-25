package com.cinx.learning.catalog.application.query;

public record SectionView(
        String id,
        String title,
        String description,
        Long duration,
        Integer orderIndex
) {
}

