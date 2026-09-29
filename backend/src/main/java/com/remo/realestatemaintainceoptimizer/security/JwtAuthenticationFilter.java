package com.remo.realestatemaintainceoptimizer.security;

import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from its session cookie, but only while the token is valid and its account still exists, has not expired and has not revoked that session by logging out.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<SimpleGrantedAuthority> USER_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtService jwtService;
    private final SessionCookieManager sessionCookieManager;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService, SessionCookieManager sessionCookieManager, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.sessionCookieManager = sessionCookieManager;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        sessionCookieManager.readToken(request)
                .flatMap(jwtService::parseToken)
                .flatMap(this::loadAcceptedAccount)
                .map(AuthenticatedUser::from)
                .ifPresent(this::authenticate);

        filterChain.doFilter(request, response);
    }

    private Optional<User> loadAcceptedAccount(JwtService.SessionToken token) {
        return userRepository.findById(token.userId())
                .filter(user -> !user.isExpiredAt(Instant.now()))
                .filter(user -> user.acceptsSessionStartedAt(token.sessionStart()));
    }

    private void authenticate(AuthenticatedUser principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, USER_AUTHORITIES));
        SecurityContextHolder.setContext(context);
    }
}
