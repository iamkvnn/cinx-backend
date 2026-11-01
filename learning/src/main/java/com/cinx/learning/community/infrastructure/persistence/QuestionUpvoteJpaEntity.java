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

@Entity(name = "QuestionUpvote")
@Table(
    name = "question_upvotes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"questionId", "userId"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUpvoteJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String questionId;

    @Column(nullable = false)
    private String userId;
}
