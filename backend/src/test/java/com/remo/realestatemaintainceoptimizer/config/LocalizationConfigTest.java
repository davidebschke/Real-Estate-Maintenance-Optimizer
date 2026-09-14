package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Verifies that dynamic backend messages resolve for German and English and fall back to English in case of doubt.
 */
@SpringBootTest
class LocalizationConfigTest {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private LocaleResolver localeResolver;

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
        Locale iso639PrivateUseLocale = Locale.forLanguageTag("xx-XX");

        String message = messageSource.getMessage("common.error.generic", null, iso639PrivateUseLocale);

        assertThat(message).isEqualTo("An unexpected error occurred.");
    }

    @Test
    void throwsForMissingKey() {
        assertThatThrownBy(() -> messageSource.getMessage("common.error.doesNotExist", null, Locale.GERMAN))
                .isInstanceOf(NoSuchMessageException.class);
    }

    @Test
    void resolvesEnglishWhenAcceptLanguageHeaderIsMissing() {
        Locale resolvedLocale = localeResolver.resolveLocale(new MockHttpServletRequest());

        assertThat(resolvedLocale).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void resolvesEnglishForReservedPrivateUseAcceptLanguageHeader() {
        String iso639PrivateUseLanguageTag = "xx-XX";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", iso639PrivateUseLanguageTag);

        Locale resolvedLocale = localeResolver.resolveLocale(request);

        assertThat(resolvedLocale).isEqualTo(Locale.ENGLISH);
    }
}
