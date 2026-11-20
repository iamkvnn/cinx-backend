package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import com.cinx.learning.catalog.domain.SubtitleJobType;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "SubtitleJob")
@Table(indexes = {
        @Index(name = "idx_subtitle_job_video_status", columnList = "video_lesson_id,status"),
        @Index(name = "idx_subtitle_job_target_language", columnList = "video_lesson_id,target_language_code")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleJobJpaEntity extends AuditableEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtitleJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtitleJobStatus status;

    private String sourceSubtitleId;

    private String outputSubtitleId;

    @Column(length = 35)
    private String sourceLanguageCode;

    @Column(nullable = false, length = 35)
    private String targetLanguageCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String expectedOutputFileKey;

    @Column(nullable = false)
    private Integer progressPercent;

    private String errorCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_lesson_id", nullable = false)
    private VideoLessonJpaEntity videoLesson;
}

