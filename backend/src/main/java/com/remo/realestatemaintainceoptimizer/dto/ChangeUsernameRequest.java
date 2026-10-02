package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.security.PasswordPolicy;
import com.remo.realestatemaintainceoptimizer.security.UsernamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for changing the username to one that satisfies the username policy, confirmed with the current password.
 */
public record ChangeUsernameRequest(
        @NotBlank @Pattern(regexp = UsernamePolicy.PATTERN) String username,
        @NotBlank @Size(max = PasswordPolicy.MAX_INPUT_LENGTH) String currentPassword) {
}
