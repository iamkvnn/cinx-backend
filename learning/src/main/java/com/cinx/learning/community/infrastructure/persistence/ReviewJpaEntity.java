package com.cinx.learning.community.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity(name = "Review")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "courseId"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewJpaEntity extends AuditableEntity {
    private String userId;
    private String courseId;
    private Double rating;
    private String content;

    @OneToMany(mappedBy = "reviewId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReactionJpaEntity> reactions;
}
