package com.cinx.learning.catalog.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.Category;

/** What other features may read of a Category; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class CategoryInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String name;

    public static CategoryInfo of(Category domain) {
        return CategoryInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .name(domain.getName())
                .build();
    }
}
