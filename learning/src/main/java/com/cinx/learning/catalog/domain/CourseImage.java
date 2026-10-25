package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseImage extends Model {
    private String imageUrl;
    private String publicId;
    /** Position in the course's gallery; the first image is the cover. */
    @Builder.Default
    private int sortOrder = 0;
    private String courseId;
}
