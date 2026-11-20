package com.cinx.learning.progress.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLessonTrackingHistory extends Model {
    private String userId;
    private String videoLessonId;
    private Integer currentPosition;
    private String watchedRanges;
    private LocalDateTime lastTrackingTime;
}
