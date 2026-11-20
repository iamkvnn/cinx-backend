package com.cinx.learning.community.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "CourseAnswer")
@Table(
    name = "course_answers",
    indexes = {
        @Index(name = "idx_course_a_question", columnList = "questionId, parentAnswerId")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnswerJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String questionId;

    private String parentAnswerId;

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isInstructorAnswer = false;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer upvoteCount = 0;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer depth = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;
}
