package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Verifies the length boundaries of the password policy, counting characters for the minimum and UTF-8 bytes for the maximum.
 */
class PasswordPolicyTest {

    @Test
    void aPasswordShorterThanTheMinimumLengthIsTooShort() {
        assertThat(PasswordPolicy.isTooShort("a".repeat(PasswordPolicy.MIN_LENGTH - 1))).isTrue();
        assertThat(PasswordPolicy.isTooShort("a".repeat(PasswordPolicy.MIN_LENGTH))).isFalse();
    }

    @Test
    void aPasswordIsTooLongOnlyBeyondTheBcryptByteLimit() {
        assertThat(PasswordPolicy.isTooLong("a".repeat(PasswordPolicy.MAX_BYTES))).isFalse();
        assertThat(PasswordPolicy.isTooLong("a".repeat(PasswordPolicy.MAX_BYTES + 1))).isTrue();
    }

    @Test
    void multiByteCharactersCountByTheirByteLength() {
        assertThat(PasswordPolicy.isTooLong("ä".repeat(PasswordPolicy.MAX_BYTES / 2))).isFalse();
        assertThat(PasswordPolicy.isTooLong("ä".repeat(PasswordPolicy.MAX_BYTES / 2 + 1))).isTrue();
    }
}
