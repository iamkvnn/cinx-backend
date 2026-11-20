package com.cinx.notification.notification.infrastructure.messaging.context;

import java.util.List;
import java.util.Map;

public record NotificationContext(
        List<String> channels,
        Map<String, Object> emailPayload,
        Map<String, Object> inAppPayload,
        Map<String, Object> pushPayload
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> channels;
        private Map<String, Object> emailPayload;
        private Map<String, Object> inAppPayload;
        private Map<String, Object> pushPayload;

        public Builder channels(List<String> channels) {
            this.channels = channels;
            return this;
        }

        public Builder emailPayload(Map<String, Object> emailPayload) {
            this.emailPayload = emailPayload;
            return this;
        }

        public Builder inAppPayload(Map<String, Object> inAppPayload) {
            this.inAppPayload = inAppPayload;
            return this;
        }

        public Builder pushPayload(Map<String, Object> pushPayload) {
            this.pushPayload = pushPayload;
            return this;
        }

        public NotificationContext build() {
            return new NotificationContext(channels, emailPayload, inAppPayload, pushPayload);
        }
    }
}
