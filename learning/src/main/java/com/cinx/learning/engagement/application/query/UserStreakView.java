package com.cinx.learning.engagement.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakView {
    private String userId;
    private Integer currentStreak;
    private Integer highestStreak;
    private LocalDate lastActivityDate;
}
