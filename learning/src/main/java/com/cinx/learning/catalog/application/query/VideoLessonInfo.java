package com.cinx.learning.catalog.application.query;

import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.VideoLesson;

/** What other features may read of a VideoLesson; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class VideoLessonInfo {
    String lessonId;
    String videoUrl;
    String fileKey;
    String fileName;
    String fileType;
    Long fileSize;
    Integer duration;
    String status;

    public static VideoLessonInfo of(VideoLesson domain) {
        return VideoLessonInfo.builder()
                .lessonId(domain.getLessonId())
                .videoUrl(domain.getVideoUrl())
                .fileKey(domain.getFileKey())
                .fileName(domain.getFileName())
                .fileType(domain.getFileType())
                .fileSize(domain.getFileSize())
                .duration(domain.getDuration())
                .status(domain.getStatus())
                .build();
    }
}
