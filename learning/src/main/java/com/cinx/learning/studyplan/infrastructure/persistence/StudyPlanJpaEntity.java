package com.cinx.learning.studyplan.infrastructure.persistence;

import com.cinx.learning.studyplan.domain.StudyPlanSource;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A learner's study plan ("Tuần của tôi"). Only settings are stored; lessons are placed on days on each read by
 * {@code StudyPlanScheduler}, so a missed day simply flows forward.
 */
@Entity(name = "StudyPlan")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanJpaEntity extends AuditableEntity {
    @Column(nullable = false, unique = true)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StudyPlanSource source;

    /** The personal path scheduled when {@code source = PATH}. */
    private String pathId;

    @Column(nullable = false)
    private Integer minutesPerDay;

    /** Seven flags, Monday first: "1101110" = Mon, Tue, Thu, Fri, Sat. */
    @Column(nullable = false, length = 7)
    private String freeDays;

    @Column(nullable = false)
    private LocalDate startDate;

    /** The finish date computed when the settings were last saved; a later computed finish means "late". */
    private LocalDate baselineFinishDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "study_plan_course", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "course_id", nullable = false)
    private List<String> courseIds = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "study_plan_day_off", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "day_off", nullable = false)
    private Set<LocalDate> daysOff = new HashSet<>();
}
