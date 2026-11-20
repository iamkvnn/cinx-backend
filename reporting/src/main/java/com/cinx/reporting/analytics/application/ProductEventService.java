package com.cinx.reporting.analytics.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.reporting.analytics.application.command.IncomingEvent;
import com.cinx.reporting.analytics.application.query.IngestResult;
import com.cinx.reporting.analytics.domain.AnalyticsConsentRepository;
import com.cinx.reporting.analytics.domain.EventCatalog;
import com.cinx.reporting.analytics.domain.Experiment;
import com.cinx.reporting.analytics.domain.ExperimentRepository;
import com.cinx.reporting.analytics.domain.ExperimentStatus;
import com.cinx.reporting.analytics.domain.InvalidProductEventException;
import com.cinx.reporting.analytics.domain.ProductEvent;
import com.cinx.reporting.analytics.domain.ProductEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductEventService implements IProductEventService {
    /** A client queues in memory for seconds; anything older is a replay or a broken clock. */
    static final int MAX_AGE_HOURS = 24;

    private final ProductEventRepository events;
    private final ExperimentRepository experiments;
    private final AnalyticsConsentRepository consents;
    private final EventRateLimiter limiter;
    private final Clock clock;

    @Override
    @Transactional
    public IngestResult ingest(String userId, List<IncomingEvent> batch) {
        if (batch == null || batch.isEmpty() || batch.size() > MAX_BATCH) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A batch holds 1-" + MAX_BATCH + " events");
        }
        List<Map<String, Object>> props = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            IncomingEvent event = batch.get(i);
            try {
                requireUuid(event.eventId());
                if (event.occurredAt() == null) throw new InvalidProductEventException("occurredAt is required");
                props.add(EventCatalog.clean(event.name(), event.props()));
            } catch (InvalidProductEventException invalid) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "events[" + i + "]: " + invalid.getMessage());
            }
        }
        if (!limiter.tryUse(userId, batch.size())) {
            throw new EventRateLimitedException();
        }
        if (!consents.allows(userId)) {
            return new IngestResult(0, 0, batch.size());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, Optional<Experiment>> experimentsSeen = new HashMap<>();
        List<ProductEvent> kept = new ArrayList<>();
        int dropped = 0;
        for (int i = 0; i < batch.size(); i++) {
            IncomingEvent event = batch.get(i);
            LocalDateTime occurredAt = event.occurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            if (occurredAt.isBefore(now.minusHours(MAX_AGE_HOURS))) {
                dropped++;
                continue;
            }
            if (occurredAt.isAfter(now)) occurredAt = now;
            if (EventCatalog.EXPERIMENT_EXPOSED.equals(event.name()) && !matchesRunning(userId, props.get(i), experimentsSeen)) {
                dropped++;
                continue;
            }
            kept.add(new ProductEvent(userId, event.eventId(), event.name(), occurredAt, now, props.get(i)));
        }

        int inserted = kept.isEmpty() ? 0 : events.insertNew(kept);
        for (ProductEvent event : kept) {
            if (EventCatalog.EXPERIMENT_EXPOSED.equals(event.name())) {
                events.recordFirstExposure((String) event.props().get("experimentKey"), userId,
                        (String) event.props().get("variant"), event.occurredAt());
            }
        }
        return new IngestResult(inserted, kept.size() - inserted, dropped);
    }

    // a client that still holds an assignment from before a stop, or a forged variant, is not an exposure
    private boolean matchesRunning(String userId, Map<String, Object> props, Map<String, Optional<Experiment>> seen) {
        String key = (String) props.get("experimentKey");
        Optional<Experiment> experiment = seen.computeIfAbsent(key, experiments::findByKey);
        return experiment.isPresent()
                && experiment.get().getStatus() == ExperimentStatus.RUNNING
                && experiment.get().assign(userId).equals(props.get("variant"));
    }

    private static void requireUuid(String eventId) {
        try {
            if (eventId == null || !UUID.fromString(eventId).toString().equalsIgnoreCase(eventId)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException notUuid) {
            throw new InvalidProductEventException("eventId is a UUID");
        }
    }
}
