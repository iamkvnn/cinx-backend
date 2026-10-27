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
public class LearningItemProgress extends Model {
    private String itemId;
    private Boolean isCompleted;
    private Boolean isPassed;
    private Double score;
    /** UTC; set the first time the item becomes complete (null for completions before V16). */
    private LocalDateTime completedAt;
    private String courseProgressId;
}
