package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLesson {
    private String lessonId;
    private String videoUrl;
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer duration;
    private String status;
}
