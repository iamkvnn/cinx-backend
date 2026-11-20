package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoQuizQuestionType;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity(name = "VideoQuestion")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoQuestionJpaEntity extends BaseEntity {
    
    private String questionText;
    
    @Enumerated(EnumType.STRING)
    private VideoQuizQuestionType questionType;
    
    private Integer timestampSeconds;

    @OneToMany(mappedBy = "videoQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoOptionJpaEntity> options;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lessonId")
    private VideoLessonJpaEntity videoLesson;
}

