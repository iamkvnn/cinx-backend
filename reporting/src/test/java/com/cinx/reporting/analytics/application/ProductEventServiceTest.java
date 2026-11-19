package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.reporting.analytics.application.command.IncomingEvent;
import com.cinx.reporting.analytics.application.query.IngestResult;
import com.cinx.reporting.analytics.domain.AnalyticsConsentRepository;
import com.cinx.reporting.analytics.domain.Experiment;
import com.cinx.reporting.analytics.domain.ExperimentRepository;
import com.cinx.reporting.analytics.domain.ProductEvent;
import com.cinx.reporting.analytics.domain.ProductEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductEventServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-13T09:00:00Z");
    private static final LocalDateTime NOW_UTC = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ProductEventRepository events;
    @Mock
    private ExperimentRepository experiments;
    @Mock
    private AnalyticsConsentRepository consents;
    private EventRateLimiter limiter;
    private ProductEventService service;

    @BeforeEach
    void setUp() {
        limiter = new EventRateLimiter(CLOCK);
        service = new ProductEventService(events, experiments, consents, limiter, CLOCK);
        when(consents.allows("u1")).thenReturn(true);
        when(events.insertNew(anyList())).thenAnswer(call -> ((List<?>) call.getArgument(0)).size());
    }

    @Test
    void storesTheBatchForTheTokensUserWithCleanedProps() {
        IngestResult result = service.ingest("u1", List.of(click("e1"), event("learning_action_completed",
                Map.of("action", "review_answered", "correct", true, "answer", "secret"))));

        assertThat(result).isEqualTo(new IngestResult(2, 0, 0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductEvent>> stored = ArgumentCaptor.forClass(List.class);
        verify(events).insertNew(stored.capture());
        assertThat(stored.getValue()).allMatch(e -> e.userId().equals("u1") && e.receivedAt().equals(NOW_UTC));
        assertThat(stored.getValue().get(1).props()).containsOnlyKeys("action", "correct");
    }

    @Test
    void aDuplicateEventIdIsCountedNotStoredAgain() {
        when(events.insertNew(anyList())).thenReturn(1);

        assertThat(service.ingest("u1", List.of(click("e1"), click("e2")))).isEqualTo(new IngestResult(1, 1, 0));
    }

    @Test
    void aBatchOverFiftyOrWithAnUnknownEventIsRefusedWhole() {
        List<IncomingEvent> tooMany = IntStream.range(0, 51).mapToObj(i -> click("e" + i)).toList();

        assertThatThrownBy(() -> service.ingest("u1", tooMany)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.ingest("u1", List.of(click("e1"), event("page_viewed", Map.of()))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("events[1]");
        assertThatThrownBy(() -> service.ingest("u1", List.of(new IncomingEvent("not-a-uuid", "recommendation_clicked",
                OffsetDateTime.now(CLOCK), Map.of("recommendationId", "r", "courseId", "c")))))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(events);
    }

    @Test
    void consentOffDropsEverything() {
        when(consents.allows("u1")).thenReturn(false);

        assertThat(service.ingest("u1", List.of(click("e1"), click("e2")))).isEqualTo(new IngestResult(0, 0, 2));
        verifyNoInteractions(events);
    }

    @Test
    void overTheHourlyBudgetIsRateLimited() {
        List<IncomingEvent> fifty = IntStream.range(0, 50).mapToObj(i -> click("e" + i)).toList();
        IntStream.range(0, EventRateLimiter.PER_HOUR / 50).forEach(i -> service.ingest("u1", fifty));

        assertThatThrownBy(() -> service.ingest("u1", List.of(click("x")))).isInstanceOf(EventRateLimitedException.class);
    }

    @Test
    void oldEventsAreDroppedAndFutureOnesClampedToNow() {
        IncomingEvent old = new IncomingEvent(uuid(), "recommendation_clicked", OffsetDateTime.ofInstant(NOW.minusSeconds(25 * 3600), ZoneOffset.UTC),
                Map.of("recommendationId", "r", "courseId", "c"));
        IncomingEvent ahead = new IncomingEvent(uuid(), "recommendation_clicked", OffsetDateTime.ofInstant(NOW.plusSeconds(600), ZoneOffset.ofHours(7)),
                Map.of("recommendationId", "r", "courseId", "c"));

        assertThat(service.ingest("u1", List.of(old, ahead))).isEqualTo(new IngestResult(1, 0, 1));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductEvent>> stored = ArgumentCaptor.forClass(List.class);
        verify(events).insertNew(stored.capture());
        assertThat(stored.getValue().get(0).occurredAt()).isEqualTo(NOW_UTC);
    }

    @Test
    void anExposureCountsOnlyForTheVariantARunningExperimentGives() {
        Experiment running = Experiment.draft("week_review_card", "Card", List.of(new Experiment.Variant("control", 50),
                        new Experiment.Variant("hidden", 50)), "learning_action_completed", Map.of("action", "review_answered"),
                7, "salt", "admin", NOW_UTC).start("admin", NOW_UTC);
        when(experiments.findByKey("week_review_card")).thenReturn(Optional.of(running));
        String mine = running.assign("u1");
        String other = mine.equals("control") ? "hidden" : "control";

        IngestResult result = service.ingest("u1", List.of(exposure("week_review_card", mine), exposure("week_review_card", other)));

        assertThat(result).isEqualTo(new IngestResult(1, 0, 1));
        verify(events).recordFirstExposure(eq("week_review_card"), eq("u1"), eq(mine), any());
        verify(events, never()).recordFirstExposure(any(), any(), eq(other), any());
    }

    @Test
    void anExposureToADraftOrUnknownExperimentIsDropped() {
        Experiment draft = Experiment.draft("week_review_card", "Card", List.of(new Experiment.Variant("control", 50),
                        new Experiment.Variant("hidden", 50)), "learning_action_completed", Map.of(), 7, "salt", "admin", NOW_UTC);
        when(experiments.findByKey("week_review_card")).thenReturn(Optional.of(draft));
        when(experiments.findByKey("gone")).thenReturn(Optional.empty());

        assertThat(service.ingest("u1", List.of(exposure("week_review_card", "control"), exposure("gone", "control"))))
                .isEqualTo(new IngestResult(0, 0, 2));
        verify(events, never()).recordFirstExposure(any(), any(), any(), any());
    }

    private static IncomingEvent click(String suffix) {
        return event("recommendation_clicked", Map.of("recommendationId", "r-" + suffix, "courseId", "c1", "position", 0));
    }

    private static IncomingEvent exposure(String key, String variant) {
        return event("experiment_exposed", Map.of("experimentKey", key, "variant", variant));
    }

    private static IncomingEvent event(String name, Map<String, Object> props) {
        return new IncomingEvent(uuid(), name, OffsetDateTime.ofInstant(NOW.minusSeconds(5), ZoneOffset.ofHours(7)),
                Collections.unmodifiableMap(props));
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
