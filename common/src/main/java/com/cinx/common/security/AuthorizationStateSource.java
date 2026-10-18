package com.cinx.common.security;

@FunctionalInterface
public interface AuthorizationStateSource {
    AuthorizationState load(String userId);
}
