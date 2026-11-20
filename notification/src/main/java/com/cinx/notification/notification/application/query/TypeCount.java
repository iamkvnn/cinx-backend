package com.cinx.notification.notification.application.query;

/** How many notifications of one type a user has, and how many of those are unread. A null type is untyped. */
public record TypeCount(String type, long total, long unread) {
}
