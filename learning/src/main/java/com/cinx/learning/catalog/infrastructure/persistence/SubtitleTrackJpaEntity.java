package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "SubtitleTrack")
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subtitle_track_video_language",
                        columnNames = {"video_lesson_id", "language_code"}
                )
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleTrackJpaEntity extends AuditableEntity {
    @Column(name = "language_code", nullable = false, length = 35)
    private String languageCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String fileKey;

    private String originalFileKey;

    private String wordConfidenceFileKey;

    private String wordConfidenceFileUrl;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtitleFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtitleSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtitleStatus status;

    @Column(nullable = false)
    private Boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_lesson_id", nullable = false)
    private VideoLessonJpaEntity videoLesson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String qualityReport;

    private String aiOriginalFileKey;

    @Column(length = 100)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(precision = 5, scale = 4)
    private BigDecimal editedCueShare;
}

