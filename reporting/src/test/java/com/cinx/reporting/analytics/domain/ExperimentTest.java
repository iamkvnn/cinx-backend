package com.cinx.reporting.analytics.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ExperimentTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 13, 9, 0);
    private static final Map<String, String> REVIEWED = Map.of("action", "review_answered");

    @Test
    void aRunningExperimentAssignsTheSameUserTheSameVariant() {
        Experiment running = draft(50, 50).start("admin", NOW);

        assertThat(running.assign("u1")).isEqualTo(running.assign("u1"));
        assertThat(running.toBuilder().build().assign("u2")).isEqualTo(running.assign("u2"));
    }

    @Test
    void weightsHoldOverManyUsers() {
        Experiment running = Experiment.draft("weights_check", "Weights", List.of(
                        new Experiment.Variant("control", 70), new Experiment.Variant("b", 20), new Experiment.Variant("c", 10)),
                "learning_action_completed", REVIEWED, 7, "salt-1", "admin", NOW).start("admin", NOW);
        Map<String, Integer> counts = new HashMap<>();
        IntStream.range(0, 10_000).forEach(i -> counts.merge(running.assign("user-" + i), 1, Integer::sum));

        assertThat(counts.get("control") / 10_000.0).isCloseTo(0.70, within(0.02));
        assertThat(counts.get("b") / 10_000.0).isCloseTo(0.20, within(0.02));
        assertThat(counts.get("c") / 10_000.0).isCloseTo(0.10, within(0.02));
    }

    @Test
    void anotherSaltShufflesUsers() {
        Experiment one = draft(50, 50).start("admin", NOW);
        Experiment other = one.toBuilder().salt("another").build();

        long moved = IntStream.range(0, 1_000).filter(i -> !one.assign("u" + i).equals(other.assign("u" + i))).count();
        assertThat(moved).isBetween(350L, 650L);
    }

    @Test
    void aDraftOrAStoppedExperimentGivesEveryoneControl() {
        Experiment draft = draft(1, 99);
        Experiment stopped = draft.start("admin", NOW).stop("admin", NOW.plusDays(1));

        IntStream.range(0, 200).forEach(i -> {
            assertThat(draft.assign("u" + i)).isEqualTo(Experiment.CONTROL);
            assertThat(stopped.assign("u" + i)).isEqualTo(Experiment.CONTROL);
        });
        assertThat(stopped.getStatus()).isEqualTo(ExperimentStatus.STOPPED);
        assertThat(stopped.getStoppedAt()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    void onlyADraftStartsAndOnlyARunningOneStops() {
        Experiment running = draft(50, 50).start("admin", NOW);

        assertThatThrownBy(() -> running.start("admin", NOW)).isInstanceOf(InvalidProductEventException.class);
        assertThatThrownBy(() -> draft(50, 50).stop("admin", NOW)).isInstanceOf(InvalidProductEventException.class);
        assertThatThrownBy(() -> running.stop("admin", NOW).start("admin", NOW)).isInstanceOf(InvalidProductEventException.class);
    }

    @Test
    void aDraftNeedsControlWeightsOfOneHundredAndAGoalFromTheDictionary() {
        assertThatThrownBy(() -> Experiment.draft("no_control", "x", List.of(new Experiment.Variant("a", 50), new Experiment.Variant("b", 50)),
                "learning_action_completed", REVIEWED, 7, "s", "admin", NOW)).hasMessageContaining("control");
        assertThatThrownBy(() -> draft(50, 40)).hasMessageContaining("100");
        assertThatThrownBy(() -> Experiment.draft("bad_goal", "x", variants(50, 50), "page_viewed", Map.of(), 7, "s", "admin", NOW))
                .hasMessageContaining("goal");
        assertThatThrownBy(() -> Experiment.draft("bad_filter", "x", variants(50, 50), "learning_action_completed",
                Map.of("action", "cheated"), 7, "s", "admin", NOW)).hasMessageContaining("filter");
        assertThatThrownBy(() -> Experiment.draft("Bad Key", "x", variants(50, 50), "learning_action_completed", REVIEWED, 7, "s", "admin", NOW))
                .hasMessageContaining("key");
        assertThatThrownBy(() -> Experiment.draft("window", "x", variants(50, 50), "learning_action_completed", REVIEWED, 91, "s", "admin", NOW))
                .hasMessageContaining("window");
    }

    private static Experiment draft(int control, int hidden) {
        return Experiment.draft("week_review_card", "Review card", variants(control, hidden),
                "learning_action_completed", REVIEWED, 7, "salt-1", "admin", NOW);
    }

    private static List<Experiment.Variant> variants(int control, int hidden) {
        return List.of(new Experiment.Variant("control", control), new Experiment.Variant("hidden", hidden));
    }
}
