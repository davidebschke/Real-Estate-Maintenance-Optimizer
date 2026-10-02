package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.dto.CurrentUserResponse;
import com.remo.realestatemaintainceoptimizer.dto.LoginRequest;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SessionCookieManager;
import com.remo.realestatemaintainceoptimizer.service.AuthService;
import com.remo.realestatemaintainceoptimizer.service.DemoAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes login, demo account creation, logout and the current account to the frontend, setting or clearing the session cookie.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DemoAccountService demoAccountService;
    private final JwtService jwtService;
    private final SessionCookieManager sessionCookieManager;
    private final AppointmentSchedulingProperties schedulingProperties;

    public AuthController(
            AuthService authService,
            DemoAccountService demoAccountService,
            JwtService jwtService,
            SessionCookieManager sessionCookieManager,
            AppointmentSchedulingProperties schedulingProperties) {
        this.authService = authService;
        this.demoAccountService = demoAccountService;
        this.jwtService = jwtService;
        this.sessionCookieManager = sessionCookieManager;
        this.schedulingProperties = schedulingProperties;
    }

    /**
     * Logs in with a username and password and starts a session.
     */
    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User account = authService.authenticate(request.username(), request.password(), httpRequest.getRemoteAddr());
        return withNewSession(HttpStatus.OK, account);
    }

    /**
     * Creates a demo account pre-filled with example data and starts a session for it.
     */
    @PostMapping("/demo-account")
    public ResponseEntity<CurrentUserResponse> createDemoAccount(HttpServletRequest httpRequest) {
        User account = demoAccountService.createDemoAccount(httpRequest.getRemoteAddr());
        return withNewSession(HttpStatus.CREATED, account);
    }

    /**
     * Ends the current session, deleting a demo account with all its data; succeeds even without a valid session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            authService.logout(user.id());
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookieManager.createClearingCookie().toString())
                .build();
    }

    /**
     * Returns the logged-in account.
     */
    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return CurrentUserResponse.from(authService.getAccount(user.id()), schedulingProperties);
    }

    private ResponseEntity<CurrentUserResponse> withNewSession(HttpStatus status, User account) {
        String token = jwtService.issueToken(account.id(), Instant.now());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, sessionCookieManager.createSessionCookie(token).toString())
                .body(CurrentUserResponse.from(account, schedulingProperties));
    }
}
