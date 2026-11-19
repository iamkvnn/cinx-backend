package com.cinx.gateway.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureWebTestClient
class LabSessionsRouteTest {
    @Autowired
    private RouteDefinitionLocator routes;
    @Autowired
    private WebTestClient client;

    @Test
    void labSessionsGoToTheRunnerUnchanged() {
        RouteDefinition route = routes.getRouteDefinitions().collectList().block().stream()
                .filter(r -> r.getId().equals("lab-sessions")).findFirst().orElseThrow();

        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8094");
        assertThat(route.getPredicates()).singleElement().satisfies(p -> {
            assertThat(p.getName()).isEqualTo("Path");
            assertThat(p.getArgs().values()).containsExactly("/lab-sessions/**");
        });
        assertThat(route.getFilters()).extracting(f -> f.getName()).doesNotContain("RewritePath");
    }

    @Test
    void theTicketIsTheAuthSoNoTokenIsAskedForButEveryOtherRouteStillNeedsOne() {
        int status = client.get().uri("/lab-sessions/s1/socket?ticket=t").exchange().returnResult(String.class).getStatus().value();

        assertThat(status).isNotIn(401, 403);
        client.get().uri("/api/v1/assessments/lab-sessions/s1").exchange().expectStatus().isUnauthorized();
        client.get().uri("/lab-session/s1").exchange().expectStatus().isUnauthorized();
    }
}
