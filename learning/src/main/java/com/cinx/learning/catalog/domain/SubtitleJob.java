package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleJob extends AuditedModel {
    private SubtitleJobType jobType;
    private SubtitleJobStatus status;
    private String sourceSubtitleId;
    private String outputSubtitleId;
    private String sourceLanguageCode;
    private String targetLanguageCode;
    private String displayName;
    private String expectedOutputFileKey;
    private Integer progressPercent;
    private String errorCode;
    private String errorMessage;
    private String videoLessonId;
}
