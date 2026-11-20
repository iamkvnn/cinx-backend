package com.cinx.commerce.subscription.application;

/** Whether paid subscriptions can be bought: billing is switched on and a provider is configured. */
public interface BillingAvailability {
    boolean available();
}
