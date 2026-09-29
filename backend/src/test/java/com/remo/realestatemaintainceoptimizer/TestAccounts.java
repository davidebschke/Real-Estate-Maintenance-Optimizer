package com.remo.realestatemaintainceoptimizer;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SessionCookieManager;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Creates accounts, session cookies and distinct client addresses for tests, keeping rate limits of different tests apart.
 */
public final class TestAccounts {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private static final String TEST_CSRF_TOKEN = "test-csrf-token";
    private static final AtomicInteger CLIENT_ADDRESS_COUNTER = new AtomicInteger();

    private TestAccounts() {
    }

    /**
     * Saves a regular, unlimited account with a unique username and the given password hash (null for none).
     */
    public static User saveRegularAccount(UserRepository userRepository, String passwordHash) {
        String id = UUID.randomUUID().toString();
        return userRepository.save(User.regular(
                id, "user-" + id.substring(0, 8), passwordHash, "Test User", Instant.now().truncatedTo(ChronoUnit.MICROS)));
    }

    /**
     * Saves a regular, unlimited account with a unique username and no password.
     */
    public static User saveRegularAccount(UserRepository userRepository) {
        return saveRegularAccount(userRepository, null);
    }

    /**
     * Saves a demo account with a unique username, the given expiry and the given remaining creation limits.
     */
    public static User saveDemoAccount(
            UserRepository userRepository, Instant expiresAt, int remainingPropertyCreations, int remainingAppointmentCreations) {
        String id = UUID.randomUUID().toString();
        return userRepository.save(new User(
                id,
                "demo-" + id.substring(0, 8),
                null,
                "Demo",
                true,
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                expiresAt.truncatedTo(ChronoUnit.MICROS),
                remainingPropertyCreations,
                remainingAppointmentCreations));
    }

    /**
     * Returns a session cookie that authenticates requests as the given account.
     */
    public static Cookie sessionCookie(JwtService jwtService, User account) {
        return new Cookie(SessionCookieManager.COOKIE_NAME, jwtService.issueToken(account.id(), Instant.now()));
    }

    /**
     * Returns a MockMvc running the full security filter chain whose every request is authenticated as the given account and carries a valid CSRF token.
     */
    public static MockMvc mockMvcAs(WebApplicationContext context, JwtService jwtService, User account) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").cookie(sessionCookie(jwtService, account)).with(withCsrfToken()))
                .build();
    }

    /**
     * Adds the matching CSRF cookie and header pair the single-page frontend sends, without replacing the real cookie-based token repository the way spring-security-test's {@code csrf()} does for the whole shared context.
     */
    public static RequestPostProcessor withCsrfToken() {
        return request -> {
            Cookie[] existingCookies = request.getCookies() != null ? request.getCookies() : new Cookie[0];
            Cookie[] cookies = Arrays.copyOf(existingCookies, existingCookies.length + 1);
            cookies[existingCookies.length] = new Cookie(CSRF_COOKIE_NAME, TEST_CSRF_TOKEN);
            request.setCookies(cookies);
            request.addHeader(CSRF_HEADER_NAME, TEST_CSRF_TOKEN);
            return request;
        };
    }

    /**
     * Returns a client address no other test uses, so per-client rate limits never leak between tests.
     */
    public static String uniqueClientAddress() {
        int counter = CLIENT_ADDRESS_COUNTER.incrementAndGet();
        return "10.99." + (counter / 250) + "." + (counter % 250 + 1);
    }

    /**
     * Makes a MockMvc request appear to come from the given client address.
     */
    public static RequestPostProcessor fromClient(String clientAddress) {
        return request -> {
            request.setRemoteAddr(clientAddress);
            return request;
        };
    }
}
