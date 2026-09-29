package com.remo.realestatemaintainceoptimizer.security;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Creates, clears and reads the HttpOnly session cookie that carries the session JWT, so JavaScript can never read it.
 */
@Component
public class SessionCookieManager {

    public static final String COOKIE_NAME = "remo_session";
    static final String COOKIE_PATH = "/api";

    private final AuthProperties authProperties;

    public SessionCookieManager(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Returns a cookie holding the given token for one session duration.
     */
    public ResponseCookie createSessionCookie(String token) {
        return baseCookie(token).maxAge(authProperties.sessionDuration()).build();
    }

    /**
     * Returns a cookie that makes the browser delete the session cookie immediately.
     */
    public ResponseCookie createClearingCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    /**
     * Returns the session token sent with the given request, if any.
     */
    public Optional<String> readToken(HttpServletRequest request) {
        return Optional.ofNullable(WebUtils.getCookie(request, COOKIE_NAME))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authProperties.cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
