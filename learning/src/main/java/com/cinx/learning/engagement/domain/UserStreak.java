package com.cinx.learning.engagement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak extends Model {
    private String userId;
    private Integer currentStreak;
    private Integer highestStreak;
    private LocalDate lastActivityDate;
    /** The highest milestone (7, 30, 100) already celebrated in this run of the streak; 0 when none. */
    @Builder.Default
    private Integer celebratedMilestone = 0;
    /** The last active day of a broken streak whose loss was already shown. */
    private LocalDate lostNoticedOn;
}
