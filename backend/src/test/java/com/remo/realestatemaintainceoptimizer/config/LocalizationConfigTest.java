package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * Verifies that dynamic backend messages resolve for German and English and fall back correctly for unsupported locales.
 */
@SpringBootTest
class LocalizationConfigTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    void resolvesGermanMessage() {
        String message = messageSource.getMessage("common.error.generic", null, Locale.GERMAN);

        assertThat(message).isEqualTo("Ein unerwarteter Fehler ist aufgetreten.");
    }

    @Test
    void resolvesEnglishMessage() {
        String message = messageSource.getMessage("common.error.generic", null, Locale.ENGLISH);

        assertThat(message).isEqualTo("An unexpected error occurred.");
    }

    @Test
    void fallsBackToDefaultBundleForUnsupportedLocale() {
        String message = messageSource.getMessage("common.error.generic", null, Locale.FRENCH);

        assertThat(message).isEqualTo("Ein unerwarteter Fehler ist aufgetreten.");
    }

    @Test
    void throwsForMissingKey() {
        assertThatThrownBy(() -> messageSource.getMessage("common.error.doesNotExist", null, Locale.GERMAN))
                .isInstanceOf(NoSuchMessageException.class);
    }
}
