package com.cinx.learning.dailyquiz.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A teaser question for the public home page. Not a graded assessment: its answer key is public on purpose,
 * and it must never be reused for scoring.
 */
@Entity(name = "DailyQuizQuestion")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyQuizQuestionJpaEntity extends AuditableEntity {
    @Column(nullable = false, length = 60)
    private String topic;

    @Column(nullable = false, length = 500)
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(nullable = false, length = 1)
    private String answerKey;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_quiz_option", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "sort_order")
    private List<DailyQuizOptionJpaEmbeddable> options = new ArrayList<>();
}
