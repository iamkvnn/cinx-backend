package com.cinx.learning.community.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "CourseQuestion")
@Table(
    name = "course_questions",
    indexes = {
        @Index(name = "idx_course_q_course", columnList = "courseId"),
        @Index(name = "idx_course_q_lesson", columnList = "lessonId")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseQuestionJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String courseId;

    private String lessonId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer upvoteCount = 0;
}
