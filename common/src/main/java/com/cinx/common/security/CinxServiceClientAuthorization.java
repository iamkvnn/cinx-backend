package com.cinx.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Checks the OAuth2 authorized-party claim for method-level internal API
 * allowlists. The resource-server decoder still validates the audience and
 * token kind; this bean narrows a shared internal decoder to the clients that
 * are allowed for an individual operation.
 */
@Component("cinxServiceClient")
public class CinxServiceClientAuthorization {
    public boolean is(Authentication authentication, String... allowedClients) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        String clientId = jwtAuthentication.getToken().getClaimAsString("azp");
        return clientId != null && Arrays.asList(allowedClients).contains(clientId);
    }
}
