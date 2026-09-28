package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies that persistence conflicts surfacing at commit time are answered with a localized 409 instead of a generic 500.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("locales/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    void anOptimisticLockingFailureReturnsALocalizedConflict() throws Exception {
        mockMvc.perform(get("/optimistic-locking-failure").header("Accept-Language", "de"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo(
                        "Dieser Eintrag wurde zwischenzeitlich von jemand anderem geändert. Bitte neu laden und erneut versuchen.")));
    }

    @Test
    void aDataIntegrityViolationReturnsALocalizedConflict() throws Exception {
        mockMvc.perform(get("/data-integrity-violation").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo(
                        "This entry was changed by someone else in the meantime. Please reload and try again.")));
    }

    @RestController
    static class FailingController {

        @GetMapping("/optimistic-locking-failure")
        String failWithOptimisticLocking() {
            throw new ObjectOptimisticLockingFailureException("Appointment", "appointment-1");
        }

        @GetMapping("/data-integrity-violation")
        String failWithDataIntegrityViolation() {
            throw new DataIntegrityViolationException("violates foreign key constraint appointments_property_id_fkey");
        }
    }
}
