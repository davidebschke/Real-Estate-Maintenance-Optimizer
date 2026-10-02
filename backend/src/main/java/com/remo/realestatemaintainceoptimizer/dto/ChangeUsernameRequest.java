package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for changing the username to 3 to 50 letters, digits, dots, hyphens or underscores, not starting with the prefix reserved for demo accounts, confirmed with the current password.
 */
public record ChangeUsernameRequest(
        @NotBlank @Pattern(regexp = "^(?!(?i)demo-)[A-Za-z0-9._-]{3,50}$") String username,
        @NotBlank @Size(max = 128) String currentPassword) {
}
