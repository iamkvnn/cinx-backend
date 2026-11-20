package com.cinx.commerce.cart.infrastructure.messaging.event;

public record LearningPathCheckoutRequested(String eventId, String userId, String courseId) {
}
