package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleTrack extends AuditedModel {
    private String languageCode;
    private String displayName;
    private String fileUrl;
    private String fileKey;
    private String originalFileKey;
    private String wordConfidenceFileKey;
    private String wordConfidenceFileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private SubtitleFormat format;
    private SubtitleSource source;
    private SubtitleStatus status;
    private Boolean isDefault;
    private String videoLessonId;
    /** The worker's quality summary as JSON; null for a track a person made. */
    private String qualityReport;
    /** The AI output as delivered; edits write new files, so this stays to measure what the reviewer changed. */
    private String aiOriginalFileKey;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    /** Share of the AI cues the reviewer changed before approving. */
    private BigDecimal editedCueShare;

    public boolean isAiOutput() {
        return source == SubtitleSource.AI_GENERATED
                || source == SubtitleSource.AI_TRANSLATED
                || source == SubtitleSource.AI_EDITED;
    }

    public boolean isVisibleToLearners() {
        return status == SubtitleStatus.READY;
    }
}
