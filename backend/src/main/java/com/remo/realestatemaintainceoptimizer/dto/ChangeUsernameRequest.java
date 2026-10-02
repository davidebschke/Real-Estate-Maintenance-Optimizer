package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for changing the username to 3 to 50 letters, digits, dots, hyphens or underscores, not starting with the prefix reserved for demo accounts.
 */
public record ChangeUsernameRequest(
        @NotBlank @Pattern(regexp = "^(?!(?i)demo-)[A-Za-z0-9._-]{3,50}$") String username) {
}
