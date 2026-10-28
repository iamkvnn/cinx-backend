package com.cinx.learning.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity(name = "VideoLesson")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLessonJpaEntity {
    @Id
    private String lessonId;

    private String videoUrl;
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;

    private Integer duration;
    private String status;

    @OneToMany(mappedBy = "videoLesson", fetch = FetchType.LAZY)
    private List<VideoQuestionJpaEntity> questions;

    @OneToMany(mappedBy = "videoLesson", fetch = FetchType.LAZY)
    @OrderBy("isDefault DESC, languageCode ASC")
    private List<SubtitleTrackJpaEntity> subtitles;
}

