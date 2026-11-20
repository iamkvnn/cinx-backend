package com.cinx.learning.learningpath.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "LearningPathItem")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemJpaEntity extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "learning_path_id")
    private UserLearningPathJpaEntity learningPath;

    private String courseId;
    private String lessonId;
    
    private Integer orderIndex;
    private String phase;
    private Boolean isSuggested;
    private Boolean isCompleted;
}
