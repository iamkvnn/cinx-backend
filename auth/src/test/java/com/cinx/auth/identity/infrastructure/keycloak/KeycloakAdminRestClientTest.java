package com.cinx.auth.identity.infrastructure.keycloak;

import com.cinx.auth.identity.domain.AuthorizationRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminRestClientTest {
    private static final String BASE = "http://keycloak.test";
    private static final String USER = BASE + "/admin/realms/cinx/users/subject-1";

    private MockRestServiceServer server;
    private KeycloakAdminRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminRestClient(builder, BASE, "cinx", "cinx-auth-admin", "secret");
        server.expect(requestTo(BASE + "/realms/cinx/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
    }

    /** Keycloak 26 validates a user PUT against the user profile, so the whole user goes back, not just the flag. */
    @Test
    void setEnabledSendsTheWholeUserBackWithTheNewFlag() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("""
                        {"id":"subject-1","username":"a@example.test","email":"a@example.test","firstName":"A","lastName":"B",
                         "enabled":true,"attributes":{"locale":["vi"]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.firstName").value("A"))
                .andExpect(jsonPath("$.attributes.locale[0]").value("vi"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andRespond(withNoContent());

        client.setEnabled("subject-1", false);

        server.verify();
    }

    @Test
    void findUserIsEmptyWhenKeycloakDoesNotKnowTheSubject() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findUser("subject-1")).isEmpty();
    }

    @Test
    void effectiveRolesKeepOnlyCinxRolesFromTheCompositeMapping() {
        server.expect(requestTo(USER + "/role-mappings/realm/composite")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"name\":\"default-roles-cinx\"},{\"name\":\"offline_access\"},{\"name\":\"LEARNER\"}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.effectiveRoles("subject-1")).containsExactly(AuthorizationRole.LEARNER);
    }

    @Test
    void addRolesPostsTheRoleRepresentationsKeycloakLists() {
        server.expect(requestTo(BASE + "/admin/realms/cinx/roles/INSTRUCTOR")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"r1\",\"name\":\"INSTRUCTOR\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[{\"id\":\"r1\",\"name\":\"INSTRUCTOR\"}]"))
                .andRespond(withNoContent());

        client.addRoles("subject-1", Set.of(AuthorizationRole.INSTRUCTOR));

        server.verify();
    }

    @Test
    void removeRolesDeletesOnlyTheGivenRoles() {
        server.expect(requestTo(BASE + "/admin/realms/cinx/roles/INSTRUCTOR")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"r1\",\"name\":\"INSTRUCTOR\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("INSTRUCTOR"))
                .andRespond(withNoContent());

        client.removeRoles("subject-1", Set.of(AuthorizationRole.INSTRUCTOR));

        server.verify();
    }

    @Test
    void enabledUsersWithRoleSkipsDisabledUsers() {
        server.expect(requestTo(BASE + "/admin/realms/cinx/roles/CONTENT_REVIEWER/users?first=0&max=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"u1\",\"enabled\":true},{\"id\":\"u2\",\"enabled\":false}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.enabledUsersWithRole(AuthorizationRole.CONTENT_REVIEWER)).containsExactly("u1");
    }
}
