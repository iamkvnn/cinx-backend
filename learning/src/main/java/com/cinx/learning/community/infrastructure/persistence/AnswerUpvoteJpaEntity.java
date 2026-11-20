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

@Entity(name = "AnswerUpvote")
@Table(
    name = "answer_upvotes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"answerId", "userId"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerUpvoteJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String answerId;

    @Column(nullable = false)
    private String userId;
}
