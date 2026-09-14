package com.remo.realestatemaintainceoptimizer.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resolves the request locale from the {@code Accept-Language} header for translating dynamic backend text.
 */
@Configuration
public class LocalizationConfig {

    /**
     * Restricts locale resolution to German and English, defaulting to German when no supported locale is requested.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.GERMAN);
        localeResolver.setSupportedLocales(List.of(Locale.GERMAN, Locale.ENGLISH));
        return localeResolver;
    }
}
