package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for changing the username to letters, digits, dots, hyphens or underscores within the length limits, not starting with the prefix reserved for demo accounts, confirmed with the current password.
 */
public record ChangeUsernameRequest(
        @NotBlank
        @Pattern(regexp = "^(?!(?i)" + User.DEMO_USERNAME_PREFIX + ")[A-Za-z0-9._-]{"
                + User.MIN_USERNAME_LENGTH + "," + User.MAX_USERNAME_LENGTH + "}$")
        String username,
        @NotBlank @Size(max = PasswordPolicy.MAX_INPUT_LENGTH) String currentPassword) {
}
