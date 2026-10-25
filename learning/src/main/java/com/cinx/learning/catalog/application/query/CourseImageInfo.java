package com.cinx.learning.catalog.application.query;

import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.CourseImage;

/** What other features may read of a CourseImage; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class CourseImageInfo {
    String id;
    Long version;
    String imageUrl;
    String publicId;
    int sortOrder;
    String courseId;

    public static CourseImageInfo of(CourseImage domain) {
        return CourseImageInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .imageUrl(domain.getImageUrl())
                .publicId(domain.getPublicId())
                .sortOrder(domain.getSortOrder())
                .courseId(domain.getCourseId())
                .build();
    }
}
