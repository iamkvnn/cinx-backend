package com.cinx.commerce.subscription.application.command;

import java.time.LocalDateTime;
import java.util.Map;

/** A staff adjustment of one subscription: an override of one feature, or a note on the subscription as a whole. */
public record AdjustSubscriptionCommand(String featureKey, Map<String, Object> value, String reason,
                                        LocalDateTime effectiveFrom, LocalDateTime expiresAt) {
}
