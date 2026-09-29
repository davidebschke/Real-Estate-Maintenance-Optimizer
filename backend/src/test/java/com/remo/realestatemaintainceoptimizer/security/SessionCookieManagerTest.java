package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Verifies that the session cookie is HttpOnly, SameSite=Strict, scoped to the API path, secure when configured, and read back correctly.
 */
class SessionCookieManagerTest {

    private final SessionCookieManager secureManager = new SessionCookieManager(properties(true));

    @Test
    void createsAHardenedSessionCookieLastingOneSession() {
        ResponseCookie cookie = secureManager.createSessionCookie("token-value");

        assertThat(cookie.getName()).isEqualTo(SessionCookieManager.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void omitsTheSecureFlagOnlyWhenConfiguredForPlainHttp() {
        ResponseCookie cookie = new SessionCookieManager(properties(false)).createSessionCookie("token-value");

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void createsAClearingCookieThatExpiresImmediately() {
        ResponseCookie cookie = secureManager.createClearingCookie();

        assertThat(cookie.getName()).isEqualTo(SessionCookieManager.COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/api");
    }

    @Test
    void readsTheTokenFromTheSessionCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie(SessionCookieManager.COOKIE_NAME, "token-value"));

        assertThat(secureManager.readToken(request)).contains("token-value");
    }

    @Test
    void readsNothingWithoutOrWithABlankSessionCookie() {
        MockHttpServletRequest withoutCookie = new MockHttpServletRequest();
        MockHttpServletRequest withBlankCookie = new MockHttpServletRequest();
        withBlankCookie.setCookies(new Cookie(SessionCookieManager.COOKIE_NAME, " "));

        assertThat(secureManager.readToken(withoutCookie)).isEmpty();
        assertThat(secureManager.readToken(withBlankCookie)).isEmpty();
    }

    private static AuthProperties properties(boolean cookieSecure) {
        return new AuthProperties(
                "unused", Duration.ofHours(8), cookieSecure, null, 5, 20, Duration.ofMinutes(15), 10, Duration.ofHours(1), 100);
    }
}
