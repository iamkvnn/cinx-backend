package com.cinx.commerce.subscription.application;

/** Port: the local copy of the auth service's verdict on whether an account may hold paid access. */
public interface AuthEligibility {
    /** Whether the user's account is ACTIVE; auth announces only changes, so an account never heard of is. */
    boolean isActive(String userId);
}
