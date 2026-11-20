package com.cinx.learning.engagement.domain;

import lombok.AllArgsConstructor;
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
}
