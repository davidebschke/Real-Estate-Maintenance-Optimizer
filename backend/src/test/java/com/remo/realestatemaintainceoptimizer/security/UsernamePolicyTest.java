package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Verifies which usernames the pattern of the username policy accepts, in particular the length limits and the prefix reserved for demo accounts.
 */
class UsernamePolicyTest {

    private static final Pattern PATTERN = Pattern.compile(UsernamePolicy.PATTERN);

    @Test
    void acceptsLettersDigitsDotsHyphensAndUnderscoresWithinTheLengthLimits() {
        assertThat(PATTERN.matcher("abc").matches()).isTrue();
        assertThat(PATTERN.matcher("David.Ebschke_1-x").matches()).isTrue();
        assertThat(PATTERN.matcher("a".repeat(UsernamePolicy.MAX_LENGTH)).matches()).isTrue();
    }

    @Test
    void rejectsNamesOutsideTheLengthLimits() {
        assertThat(PATTERN.matcher("a".repeat(UsernamePolicy.MIN_LENGTH - 1)).matches()).isFalse();
        assertThat(PATTERN.matcher("a".repeat(UsernamePolicy.MAX_LENGTH + 1)).matches()).isFalse();
    }

    @Test
    void rejectsOtherCharacters() {
        assertThat(PATTERN.matcher("with space").matches()).isFalse();
        assertThat(PATTERN.matcher("ümlaut").matches()).isFalse();
    }

    @Test
    void rejectsTheDemoPrefixInAnyCase() {
        assertThat(PATTERN.matcher("demo-abc").matches()).isFalse();
        assertThat(PATTERN.matcher("DEMO-abc").matches()).isFalse();
        assertThat(PATTERN.matcher("demonstrator").matches()).isTrue();
    }
}
