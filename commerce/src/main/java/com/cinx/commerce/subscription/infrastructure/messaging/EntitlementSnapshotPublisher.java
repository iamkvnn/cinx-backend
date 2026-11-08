package com.cinx.commerce.subscription.infrastructure.messaging;

import com.cinx.commerce.shared.application.DomainEvents;
import com.cinx.commerce.subscription.application.EntitlementSnapshots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntitlementSnapshotPublisher implements EntitlementSnapshots {
    private final DomainEvents outbox;

    @Override
    public void publish(String subjectId, Map<String, Object> snapshot) {
        outbox.publish("cinx.domain.events", "commerce.subscription.entitlements.changed", 1,
                "commerce.subscription.entitlements.changed", subjectId, snapshot, Map.of());
    }
}
