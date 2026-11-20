package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.reporting.analytics.application.query.EventDefinitionView;
import com.cinx.reporting.analytics.application.query.FunnelView;
import com.cinx.reporting.analytics.domain.EventCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FunnelService implements IFunnelService {
    private final AnalyticsQueries queries;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FunnelView funnel(List<String> steps, LocalDate from, LocalDate to) {
        if (steps == null || steps.size() < MIN_STEPS || steps.size() > MAX_STEPS) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A funnel has " + MIN_STEPS + "-" + MAX_STEPS + " steps");
        }
        for (int i = 0; i < steps.size(); i++) {
            if (!EventCatalog.knows(steps.get(i))) throw new BadRequestException(ErrorCode.BAD_REQUEST, "Unknown event: " + steps.get(i));
            // the same event twice in a row would count one event as both steps
            if (i > 0 && steps.get(i).equals(steps.get(i - 1))) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "Two steps in a row are the same event");
            }
        }
        LocalDate today = LocalDate.now(clock.withZone(AnalyticsTime.ZONE));
        if (from == null || to == null || from.isAfter(to) || to.isAfter(today)
                || from.isBefore(today.minusDays(AnalyticsRetentionService.RAW_DAYS))) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID,
                    "The range runs forward, ends today at the latest and starts within " + AnalyticsRetentionService.RAW_DAYS + " days");
        }
        List<Long> users = queries.funnel(steps, AnalyticsTime.startOf(from), AnalyticsTime.startOf(to.plusDays(1)));
        List<FunnelView.Step> result = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            result.add(new FunnelView.Step(steps.get(i), users.get(i)));
        }
        return new FunnelView(from, to, result);
    }

    @Override
    public List<EventDefinitionView> catalog() {
        return EventCatalog.names().stream()
                .map(name -> new EventDefinitionView(name, EventCatalog.props(name).entrySet().stream()
                        .map(prop -> new EventDefinitionView.Prop(prop.getKey(), prop.getValue().type().name(),
                                prop.getValue().required(), prop.getValue().values()))
                        .toList()))
                .toList();
    }
}
