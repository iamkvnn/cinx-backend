package com.cinx.learning.studyplan.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A learner's study plan ("Tuần của tôi"). Only settings are stored; lessons are placed on days on each read by
 * {@code StudyPlanScheduler}, so a missed day simply flows forward.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlan extends AuditedModel {
    private String userId;
    private StudyPlanSource source;
    /** The personal path scheduled when {@code source = PATH}. */
    private String pathId;
    private Integer minutesPerDay;
    /** Seven flags, Monday first: "1101110" = Mon, Tue, Thu, Fri, Sat. */
    private String freeDays;
    private LocalDate startDate;
    /** The finish date computed when the settings were last saved; a later computed finish means "late". */
    private LocalDate baselineFinishDate;
    @Builder.Default
    private List<String> courseIds = new ArrayList<>();
    @Builder.Default
    private Set<LocalDate> daysOff = new HashSet<>();

    public Set<DayOfWeek> freeWeekdays() {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < Math.min(7, freeDays.length()); i++) {
            if (freeDays.charAt(i) == '1') {
                days.add(DayOfWeek.of(i + 1));
            }
        }
        return days;
    }

    public static String encodeFreeDays(Set<DayOfWeek> days) {
        StringBuilder out = new StringBuilder(7);
        for (DayOfWeek day : DayOfWeek.values()) {
            out.append(days.contains(day) ? '1' : '0');
        }
        return out.toString();
    }
}
