package com.cinx.learning.dailyquiz.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A teaser question for the public home page. Not a graded assessment: its answer key is public on purpose,
 * and it must never be reused for scoring.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyQuizQuestion extends AuditedModel {
    private String topic;
    private String prompt;
    private String codeSnippet;
    private String explanation;
    private String answerKey;
    private Integer sortOrder;
    @Builder.Default
    private Boolean active = true;
    @Builder.Default
    private List<DailyQuizOption> options = new ArrayList<>();
}
