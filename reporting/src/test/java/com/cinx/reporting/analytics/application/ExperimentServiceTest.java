package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.reporting.analytics.application.command.CreateExperimentCommand;
import com.cinx.reporting.analytics.application.command.CreateExperimentCommand.VariantWeight;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView.VariantResult;
import com.cinx.reporting.analytics.domain.Experiment;
import com.cinx.reporting.analytics.domain.ExperimentRepository;
import com.cinx.reporting.analytics.domain.ExperimentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-13T09:00:00Z");
    private static final LocalDateTime NOW_UTC = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Map<String, String> REVIEWED = Map.of("action", "review_answered");

    @Mock
    private ExperimentRepository experiments;
    @Mock
    private AnalyticsQueries queries;
    private ExperimentService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentService(experiments, queries, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aNewExperimentIsADraftWithItsOwnSalt() {
        when(experiments.findByKey("week_review_card")).thenReturn(Optional.empty());
        when(experiments.save(any())).thenAnswer(call -> call.getArgument(0));

        var created = service.create(command("week_review_card"), "admin");

        assertThat(created.status()).isEqualTo("DRAFT");
        assertThat(created.updatedBy()).isEqualTo("admin");
        assertThat(created.goalFilter()).isEqualTo(REVIEWED);
    }

    @Test
    void aTakenKeyOrBrokenRulesAreRefused() {
        when(experiments.findByKey("taken")).thenReturn(Optional.of(running("taken")));
        when(experiments.findByKey("no_control")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command("taken"), "admin")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.create(new CreateExperimentCommand("no_control", "x",
                List.of(new VariantWeight("a", 50), new VariantWeight("b", 50)), "learning_action_completed", REVIEWED, 7), "admin"))
                .isInstanceOf(BadRequestException.class);
        verify(experiments, never()).save(any());
    }

    @Test
    void stoppingIsTheKillSwitchAndUnknownKeysAreNotFound() {
        when(experiments.findByKey("week_review_card")).thenReturn(Optional.of(running("week_review_card")));
        when(experiments.findByKey("gone")).thenReturn(Optional.empty());
        when(experiments.save(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.stop("week_review_card", "admin").status()).isEqualTo(ExperimentStatus.STOPPED.name());
        assertThatThrownBy(() -> service.start("gone", "admin")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void assignmentsListOnlyRunningExperiments() {
        Experiment running = running("week_review_card");
        when(experiments.findRunning()).thenReturn(List.of(running));

        assertThat(service.assignments("u1")).singleElement()
                .satisfies(a -> assertThat(a.variant()).isEqualTo(running.assign("u1")));
    }

    @Test
    void resultsListEveryVariantEvenBeforeAnyoneSawIt() {
        when(experiments.findByKey("week_review_card")).thenReturn(Optional.of(running("week_review_card")));
        when(queries.experimentResults("week_review_card", "learning_action_completed", REVIEWED, 7))
                .thenReturn(List.of(new VariantResult("hidden", 40, 12)));

        assertThat(service.results("week_review_card").variants()).containsExactly(
                new VariantResult("control", 0, 0), new VariantResult("hidden", 40, 12));
    }

    private static CreateExperimentCommand command(String key) {
        return new CreateExperimentCommand(key, "Review card on the week page",
                List.of(new VariantWeight("control", 50), new VariantWeight("hidden", 50)), "learning_action_completed", REVIEWED, 7);
    }

    private static Experiment running(String key) {
        return Experiment.draft(key, "Card", List.of(new Experiment.Variant("control", 50), new Experiment.Variant("hidden", 50)),
                "learning_action_completed", REVIEWED, 7, "salt", "admin", NOW_UTC).start("admin", NOW_UTC);
    }
}
