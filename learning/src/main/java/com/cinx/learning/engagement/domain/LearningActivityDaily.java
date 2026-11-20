package com.cinx.learning.engagement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningActivityDaily extends Model {
    private String userId;
    private String courseId;
    private LocalDate activityDate;
    private Long activeSeconds;
    private LocalDateTime lastActivityAt;
    /** Lesson (stable id) of the latest heartbeat that named one; drives "Học tiếp". */
    private String lastItemId;
}
