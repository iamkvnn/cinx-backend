package com.cinx.learning.progress.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "VideoLessonTrackingHistory")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_lesson_id"}))
public class VideoLessonTrackingHistoryJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String videoLessonId;
    @Column(nullable = false)
    private Integer currentPosition;
    @Column(columnDefinition = "TEXT")
    private String watchedRanges;
    @Column(nullable = false)
    private LocalDateTime lastTrackingTime;
}
