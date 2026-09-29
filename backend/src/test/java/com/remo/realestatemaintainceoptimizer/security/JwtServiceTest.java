package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that session tokens round-trip with their session start, expire, and are rejected when forged, tampered with, unsigned, incomplete, or signed with another key.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256";

    private final JwtService jwtService = new JwtService(propertiesWithSecret(SECRET));

    @Test
    void returnsTheAccountIdOfAnIssuedToken() {
        String token = jwtService.issueToken("user-1", Instant.now());

        assertThat(jwtService.parseToken(token).map(JwtService.SessionToken::userId)).contains("user-1");
    }

    @Test
    void recordsTheSessionStartWithMillisecondPrecision() {
        Instant issuedAt = Instant.parse("2026-09-29T10:00:00.123456Z");
        JwtService longLivedService = new JwtService(propertiesWithSecret(SECRET, Duration.ofDays(36500)));

        String token = longLivedService.issueToken("user-1", issuedAt);

        assertThat(longLivedService.parseToken(token).map(JwtService.SessionToken::sessionStart))
                .contains(Instant.parse("2026-09-29T10:00:00.123Z"));
        assertThat(JwtService.revocationInstant(issuedAt)).isEqualTo(Instant.parse("2026-09-29T10:00:00.123Z"));
    }

    @Test
    void rejectsASignedTokenWithoutSessionStart() {
        String token = Jwts.builder()
                .issuer(JwtService.ISSUER)
                .subject("user-1")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = jwtService.issueToken("user-1", Instant.now().minus(Duration.ofHours(9)));

        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        JwtService otherService = new JwtService(propertiesWithSecret("another-secret-that-is-also-long-enough-xyz"));
        String foreignToken = otherService.issueToken("user-1", Instant.now());

        assertThat(jwtService.parseToken(foreignToken)).isEmpty();
    }

    @Test
    void rejectsATokenWhosePayloadWasTamperedWith() {
        String[] parts = jwtService.issueToken("user-1", Instant.now()).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"remo\",\"sub\":\"admin\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));

        assertThat(jwtService.parseToken(parts[0] + "." + forgedPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void rejectsAnUnsignedToken() {
        String unsignedToken = Jwts.builder()
                .issuer(JwtService.ISSUER)
                .subject("user-1")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .compact();

        assertThat(jwtService.parseToken(unsignedToken)).isEmpty();
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        String token = Jwts.builder()
                .issuer("someone-else")
                .subject("user-1")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-jwt", "a.b.c"})
    void rejectsMalformedTokens(String token) {
        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"only-31-bytes-long-secret-value"})
    void refusesToStartWithAMissingOrTooShortSecret(String secret) {
        assertThatThrownBy(() -> new JwtService(propertiesWithSecret(secret)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REMO_AUTH_JWT_SECRET");
    }

    @Test
    void exposesTheConfiguredSessionDuration() {
        assertThat(jwtService.sessionDuration()).isEqualTo(Duration.ofHours(8));
    }

    private static AuthProperties propertiesWithSecret(String secret) {
        return propertiesWithSecret(secret, Duration.ofHours(8));
    }

    private static AuthProperties propertiesWithSecret(String secret, Duration sessionDuration) {
        return new AuthProperties(
                secret, sessionDuration, true, null, 5, 20, Duration.ofMinutes(15), 10, Duration.ofHours(1), 100);
    }
}
