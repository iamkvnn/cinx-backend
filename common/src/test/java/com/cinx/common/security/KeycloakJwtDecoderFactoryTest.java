package com.cinx.common.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeycloakJwtDecoderFactoryTest {
    private static HttpServer jwks;
    private static KeyPair signingKey;
    private static KeyPair wrongKey;
    private static String jwksUri;

    @BeforeAll
    static void startJwks() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKey = generator.generateKeyPair();
        wrongKey = generator.generateKeyPair();
        RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) signingKey.getPublic()).keyID("cinx-test").build();
        byte[] body = ("{\"keys\":[" + publicJwk.toPublicJWK().toJSONString() + "]}")
                .getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        jwks.start();
        jwksUri = "http://127.0.0.1:" + jwks.getAddress().getPort() + "/jwks";
    }

    @AfterAll
    static void stopJwks() {
        jwks.stop(0);
    }

    @Test
    void rejectsMissingIssuer() {
        assertThrows(IllegalArgumentException.class,
                () -> KeycloakJwtDecoderFactory.create(" ", "http://keycloak/jwks", "cinx-api"));
    }

    @Test
    void rejectsMissingAudience() {
        assertThrows(IllegalArgumentException.class,
                () -> KeycloakJwtDecoderFactory.create("http://issuer", "http://keycloak/jwks", ""));
    }

    @Test
    void rejectsMissingJwksUri() {
        assertThrows(IllegalArgumentException.class,
                () -> KeycloakJwtDecoderFactory.create("http://issuer", " ", "cinx-api"));
    }

    @Test
    void acceptsValidImportedOrNewKeycloakToken() throws Exception {
        var decoder = KeycloakJwtDecoderFactory.create("https://id.cinx.test/realms/cinx", jwksUri, "cinx-api");
        assertEquals("keycloak-subject", decoder.decode(token(signingKey, "https://id.cinx.test/realms/cinx",
                List.of("cinx-api"), Instant.now().plusSeconds(300))).getSubject());
    }

    @Test
    void rejectsWrongSignatureIssuerAudienceAndExpiry() throws Exception {
        var decoder = KeycloakJwtDecoderFactory.create("https://id.cinx.test/realms/cinx", jwksUri, "cinx-api");
        assertThrows(JwtException.class, () -> decoder.decode(token(wrongKey,
                "https://id.cinx.test/realms/cinx", List.of("cinx-api"), Instant.now().plusSeconds(300))));
        assertThrows(JwtException.class, () -> decoder.decode(token(signingKey,
                "https://evil.test/realms/cinx", List.of("cinx-api"), Instant.now().plusSeconds(300))));
        assertThrows(JwtException.class, () -> decoder.decode(token(signingKey,
                "https://id.cinx.test/realms/cinx", List.of("another-api"), Instant.now().plusSeconds(300))));
        assertThrows(JwtException.class, () -> decoder.decode(token(signingKey,
                "https://id.cinx.test/realms/cinx", List.of("cinx-api"), Instant.now().minusSeconds(1))));
    }

    private static String token(KeyPair key, String issuer, List<String> audience, Instant expiresAt) throws Exception {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("cinx-test").build(),
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .subject("keycloak-subject")
                        .audience(audience)
                        .claim("token_kind", "user")
                        .issueTime(Date.from(now.minusSeconds(1)))
                        .expirationTime(Date.from(expiresAt))
                        .build());
        jwt.sign(new RSASSASigner(key.getPrivate()));
        return jwt.serialize();
    }
}
