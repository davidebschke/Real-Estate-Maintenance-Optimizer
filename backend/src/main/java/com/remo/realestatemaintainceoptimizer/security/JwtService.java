package com.remo.realestatemaintainceoptimizer.security;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the HMAC-signed session JWT that identifies an account and when its session started.
 */
@Service
public class JwtService {

    static final String ISSUER = "remo";
    static final String SESSION_START_CLAIM = "sst";
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration sessionDuration;
    private final JwtParser parser;

    public JwtService(AuthProperties authProperties) {
        String secret = authProperties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "remo.auth.jwt-secret (REMO_AUTH_JWT_SECRET) must be set to at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.sessionDuration = authProperties.sessionDuration();
        this.parser = Jwts.parser().verifyWith(signingKey).requireIssuer(ISSUER).build();
    }

    /**
     * Issues a token for the given account id whose session starts at {@code issuedAt} and expires one session duration later.
     */
    public String issueToken(String userId, Instant issuedAt) {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId)
                .issuedAt(Date.from(issuedAt))
                .claim(SESSION_START_CLAIM, issuedAt.toEpochMilli())
                .expiration(Date.from(issuedAt.plus(sessionDuration)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns the account id and session start of a validly signed, unexpired token, or empty for any malformed, forged or expired one.
     */
    public Optional<SessionToken> parseToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            if (claims.getSubject() == null || !(claims.get(SESSION_START_CLAIM) instanceof Number sessionStart)) {
                return Optional.empty();
            }
            return Optional.of(new SessionToken(claims.getSubject(), Instant.ofEpochMilli(sessionStart.longValue())));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * Returns the instant from which revoking sessions takes effect, at the precision a token records its session start with.
     */
    public static Instant revocationInstant(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }

    public Duration sessionDuration() {
        return sessionDuration;
    }

    /**
     * The verified content of a session token.
     */
    public record SessionToken(String userId, Instant sessionStart) {
    }
}
