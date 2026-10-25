package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class AuthorizationCatalog {
    private final JsonNode root;

    AuthorizationCatalog() {
        try {
            root = new ObjectMapper(new YAMLFactory()).readTree(
                    new ClassPathResource("contracts/security/cinx-authorization.yaml").getInputStream());
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load canonical authorization catalog", failure);
        }
    }

    long policyVersion() {
        return root.path("policyVersion").asLong();
    }

    Set<AuthorizationRole> roles() {
        Set<AuthorizationRole> result = EnumSet.noneOf(AuthorizationRole.class);
        root.path("roles").fieldNames().forEachRemaining(name -> result.add(AuthorizationRole.valueOf(name)));
        return Set.copyOf(result);
    }

    public Set<String> effectivePermissions(Collection<AuthorizationRole> roles) {
        Set<String> result = new TreeSet<>();
        Set<AuthorizationRole> visited = EnumSet.noneOf(AuthorizationRole.class);
        roles.forEach(role -> collect(role, visited, result));
        return Set.copyOf(result);
    }

    private void collect(AuthorizationRole role, Set<AuthorizationRole> visited, Set<String> permissions) {
        if (!visited.add(role)) return;
        JsonNode definition = root.path("roles").path(role.name());
        definition.path("permissions").forEach(value -> permissions.add(value.asText()));
        definition.path("includes").forEach(value -> collect(
                AuthorizationRole.valueOf(value.asText()), visited, permissions));
    }
}
