package com.cinx.learning.community.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "AnswerHelpfulMark")
@Table(
        name = "answer_helpful_marks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_answer_helpful_marks_answer_user", columnNames = {"answerId", "userId"})
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerHelpfulMarkJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String answerId;

    @Column(nullable = false)
    private String userId;
}
