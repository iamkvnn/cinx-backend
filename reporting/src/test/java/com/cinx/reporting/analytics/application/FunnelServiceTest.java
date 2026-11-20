package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunnelServiceTest {
    // 20:00 UTC on the 12th is already the 13th in Vietnam
    private static final Instant NOW = Instant.parse("2026-10-12T20:00:00Z");
    private static final LocalDate TODAY_VN = LocalDate.of(2026, 10, 13);

    @Mock
    private AnalyticsQueries queries;
    private FunnelService service;

    @BeforeEach
    void setUp() {
        service = new FunnelService(queries, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void vietnamDaysBecomeAUtcRangeAndEachStepKeepsItsCount() {
        List<String> steps = List.of("recommendation_shown", "recommendation_clicked");
        when(queries.funnel(steps, LocalDateTime.of(2026, 10, 9, 17, 0), LocalDateTime.of(2026, 10, 13, 17, 0)))
                .thenReturn(List.of(120L, 30L));

        var funnel = service.funnel(steps, LocalDate.of(2026, 10, 10), TODAY_VN);

        assertThat(funnel.steps()).extracting(s -> s.name() + "=" + s.users())
                .containsExactly("recommendation_shown=120", "recommendation_clicked=30");
    }

    @Test
    void stepsAndRangesOutsideTheRulesAreRefused() {
        LocalDate from = TODAY_VN.minusDays(7);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown"), from, TODAY_VN)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown", "page_viewed"), from, TODAY_VN)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown", "recommendation_shown"), from, TODAY_VN))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown", "recommendation_clicked"), TODAY_VN, from))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown", "recommendation_clicked"), from, TODAY_VN.plusDays(1)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.funnel(List.of("recommendation_shown", "recommendation_clicked"), TODAY_VN.minusDays(181), TODAY_VN))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(queries);
    }

    @Test
    void theCatalogListsEveryEventWithItsProps() {
        assertThat(service.catalog()).anySatisfy(event -> {
            assertThat(event.name()).isEqualTo("learning_action_completed");
            assertThat(event.props()).anySatisfy(prop -> {
                assertThat(prop.name()).isEqualTo("action");
                assertThat(prop.values()).contains("review_answered");
            });
        });
    }
}
