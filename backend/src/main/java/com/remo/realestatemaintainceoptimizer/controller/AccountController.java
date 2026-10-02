package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.dto.ChangeAppointmentBufferRequest;
import com.remo.realestatemaintainceoptimizer.dto.ChangePasswordRequest;
import com.remo.realestatemaintainceoptimizer.dto.ChangeUsernameRequest;
import com.remo.realestatemaintainceoptimizer.dto.CurrentUserResponse;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SessionCookieManager;
import com.remo.realestatemaintainceoptimizer.service.AccountService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the profile settings of the logged-in regular account: username, password and appointment buffer.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AppointmentSchedulingProperties schedulingProperties;
    private final JwtService jwtService;
    private final SessionCookieManager sessionCookieManager;

    public AccountController(
            AccountService accountService,
            AppointmentSchedulingProperties schedulingProperties,
            JwtService jwtService,
            SessionCookieManager sessionCookieManager) {
        this.accountService = accountService;
        this.schedulingProperties = schedulingProperties;
        this.jwtService = jwtService;
        this.sessionCookieManager = sessionCookieManager;
    }

    /**
     * Changes the username of the logged-in account after verifying its current password.
     */
    @PutMapping("/username")
    public CurrentUserResponse changeUsername(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangeUsernameRequest request) {
        return CurrentUserResponse.from(accountService.changeUsername(user.id(), request.username(), request.currentPassword()), schedulingProperties);
    }

    /**
     * Changes the password after verifying the current one, ending all other sessions and keeping this one alive with a fresh cookie.
     */
    @PutMapping("/password")
    public ResponseEntity<CurrentUserResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangePasswordRequest request) {
        User account = accountService.changePassword(user.id(), request.currentPassword(), request.newPassword());
        String token = jwtService.issueToken(account.id(), Instant.now());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieManager.createSessionCookie(token).toString())
                .body(CurrentUserResponse.from(account, schedulingProperties));
    }

    /**
     * Changes the minimum gap kept between two appointments of the logged-in account.
     */
    @PutMapping("/appointment-buffer")
    public CurrentUserResponse changeAppointmentBuffer(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangeAppointmentBufferRequest request) {
        User account = accountService.changeAppointmentBufferMinutes(user.id(), request.appointmentBufferMinutes());
        return CurrentUserResponse.from(account, schedulingProperties);
    }
}
