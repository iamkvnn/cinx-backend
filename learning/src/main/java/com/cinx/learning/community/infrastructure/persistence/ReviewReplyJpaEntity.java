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

@Entity(name = "ReviewReply")
@Table(
    name = "review_replies",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reviewId"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyJpaEntity extends AuditableEntity {
    @Column(nullable = false, unique = true)
    private String reviewId;

    @Column(nullable = false)
    private String instructorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
