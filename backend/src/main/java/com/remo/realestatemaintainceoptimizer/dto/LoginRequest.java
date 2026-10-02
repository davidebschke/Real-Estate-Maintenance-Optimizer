package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for logging in with a username and password.
 */
public record LoginRequest(
        @NotBlank @Size(max = User.MAX_USERNAME_LENGTH) String username,
        @NotBlank @Size(max = PasswordPolicy.MAX_INPUT_LENGTH) String password) {
}
