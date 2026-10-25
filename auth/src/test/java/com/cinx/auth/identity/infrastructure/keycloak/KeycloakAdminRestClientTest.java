package com.cinx.auth.identity.infrastructure.keycloak;

import com.cinx.auth.identity.domain.AuthorizationRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Keycloak 26 validates a user PUT against the user profile, so the client must send the whole user back
 * (email and name included) with the cinx attributes merged in, not just the attributes.
 */
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

    @Test
    void sendsTheWholeUserBackWithTheCinxAttributesMergedIntoTheExistingOnes() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("""
                        {"id":"subject-1","username":"a@example.test","email":"a@example.test","firstName":"A","lastName":"B",
                         "enabled":false,"attributes":{"locale":["vi"],"uid":["old-uid"]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.PUT)).andExpect(header("Authorization", "Bearer tok"))
                // what Keycloak requires and the old partial body left out
                .andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.firstName").value("A"))
                .andExpect(jsonPath("$.lastName").value("B"))
                // the flag follows the call, an attribute of another owner is kept, a cinx one is replaced
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.attributes.locale[0]").value("vi"))
                .andExpect(jsonPath("$.attributes.uid[0]").value("user-1"))
                .andExpect(jsonPath("$.attributes.authorization_version[0]").value("2"))
                .andExpect(jsonPath("$.attributes.authorization_policy_version[0]").value("7"))
                .andExpect(jsonPath("$.attributes.authz_not_after").doesNotExist())
                .andRespond(withNoContent());
        expectNoRoleChanges();

        client.synchronizeAuthorization("subject-1", "user-1", true, Set.of(AuthorizationRole.LEARNER), 2, 7, null);

        server.verify();
    }

    @Test
    void writesTheExpiryAsEpochSecondsWhenTheAuthorizationExpires() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"email\":\"a@example.test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.attributes.authz_not_after[0]").value("1790000000"))
                .andRespond(withNoContent());
        expectNoRoleChanges();

        client.synchronizeAuthorization("subject-1", "user-1", true, Set.of(AuthorizationRole.LEARNER), 1, 1, Instant.ofEpochSecond(1_790_000_000L));

        server.verify();
    }

    @Test
    void createsTheAttributesOfAUserThatHadNone() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"email\":\"a@example.test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.attributes.uid[0]").value("user-1"))
                .andRespond(withNoContent());
        expectNoRoleChanges();

        client.synchronizeAuthorization("subject-1", "user-1", true, Set.of(AuthorizationRole.LEARNER), 1, 1, null);

        server.verify();
    }

    @Test
    void removesOnlyTheCinxRolesItManagesAndLeavesKeycloaksOwn() {
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"email\":\"a@example.test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER)).andExpect(method(HttpMethod.PUT)).andRespond(withNoContent());
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"name\":\"offline_access\"},{\"name\":\"INSTRUCTOR\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("INSTRUCTOR"))
                .andRespond(withNoContent());
        server.expect(requestTo(BASE + "/admin/realms/cinx/roles/LEARNER")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"LEARNER\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[{\"name\":\"LEARNER\"}]"))
                .andRespond(withNoContent());

        client.synchronizeAuthorization("subject-1", "user-1", true, Set.of(AuthorizationRole.LEARNER), 3, 1, null);

        server.verify();
    }

    /** The user already holds no cinx role and wants one that Keycloak lists: a lookup and a mapping, no delete. */
    private void expectNoRoleChanges() {
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/admin/realms/cinx/roles/LEARNER")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"LEARNER\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/role-mappings/realm")).andExpect(method(HttpMethod.POST)).andRespond(withNoContent());
    }
}
