package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.security.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for changing the password, requiring the current password as proof.
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = PasswordPolicy.MAX_INPUT_LENGTH) String currentPassword,
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_INPUT_LENGTH) String newPassword) {
}
