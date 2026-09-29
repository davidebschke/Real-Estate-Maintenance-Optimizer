package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Verifies that only a valid session cookie of an existing, unexpired account with an unrevoked session authenticates the request.
 */
class JwtAuthenticationFilterTest {

    private static final AuthProperties PROPERTIES = new AuthProperties(
            "unit-test-secret-that-is-long-enough-for-hs256", Duration.ofHours(8), true, null,
            5, 20, Duration.ofMinutes(15), 10, Duration.ofHours(1), 100);

    private final JwtService jwtService = new JwtService(PROPERTIES);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtService, new SessionCookieManager(PROPERTIES), userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAValidSessionOfAnExistingAccount() throws Exception {
        User account = User.regular("user-1", "debschke", null, "David Ebschke", Instant.now());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(account));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken(jwtService.issueToken("user-1", Instant.now())), new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser("user-1", "debschke", "David Ebschke", false));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void leavesARequestWithoutSessionCookieUnauthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void ignoresAForgedToken() throws Exception {
        filter.doFilter(requestWithToken("forged.token.value"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenOfADeletedAccount() throws Exception {
        when(userRepository.findById("deleted")).thenReturn(Optional.empty());

        filter.doFilter(requestWithToken(jwtService.issueToken("deleted", Instant.now())),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenOfAnExpiredDemoAccount() throws Exception {
        Instant now = Instant.now();
        User expired = new User("demo-1", "demo-1", null, "Demo", true, now.minusSeconds(60), now.minusSeconds(1), 3, 3);
        when(userRepository.findById("demo-1")).thenReturn(Optional.of(expired));

        filter.doFilter(requestWithToken(jwtService.issueToken("demo-1", now.minusSeconds(30))),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenWhoseSessionWasRevokedByALaterLogout() throws Exception {
        Instant sessionStart = Instant.now().minusSeconds(60);
        User account = User.regular("user-1", "debschke", null, "David Ebschke", sessionStart);
        account.invalidateSessionsStartedBefore(Instant.now());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(account));

        filter.doFilter(requestWithToken(jwtService.issueToken("user-1", sessionStart)),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionCookieManager.COOKIE_NAME, token));
        return request;
    }
}
