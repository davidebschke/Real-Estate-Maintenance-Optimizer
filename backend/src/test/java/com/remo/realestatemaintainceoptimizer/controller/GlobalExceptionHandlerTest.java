package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCredentialsException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.exception.TenantLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
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
 * Verifies that persistence conflicts surfacing at commit time are answered with a localized 409 instead of a generic 500, and authentication and limit failures with their localized 401, 403 and 429.
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

    @Test
    void invalidCredentialsReturnALocalizedUnauthorized() throws Exception {
        mockMvc.perform(get("/invalid-credentials").header("Accept-Language", "de"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Benutzername oder Passwort ist falsch.")));
    }

    @Test
    void aMissingAccountReturnsALocalizedUnauthorized() throws Exception {
        mockMvc.perform(get("/account-not-found").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Your session has expired. Please log in again.")));
    }

    @Test
    void anExceededRateLimitReturnsALocalizedTooManyRequests() throws Exception {
        mockMvc.perform(get("/rate-limit-exceeded").header("Accept-Language", "de"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", equalTo(
                        "Zu viele fehlgeschlagene Anmeldeversuche. Bitte versuchen Sie es in einigen Minuten erneut.")));
    }

    @Test
    void anExhaustedCreationLimitReturnsALocalizedForbidden() throws Exception {
        mockMvc.perform(get("/creation-quota-exceeded").header("Accept-Language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("A demo account can create at most 3 additional properties.")));
    }

    @Test
    void anExceededRouteRequestLimitReturnsALocalizedTooManyRequests() throws Exception {
        mockMvc.perform(get("/route-rate-limit-exceeded").header("Accept-Language", "en"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", equalTo("Too many route calculations. Please try again in a minute.")));
    }

    @Test
    void anUnavailableRoutingReturnsALocalizedServiceUnavailable() throws Exception {
        mockMvc.perform(get("/routing-unavailable").header("Accept-Language", "de"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", equalTo("Die Straßenroute konnte gerade nicht berechnet werden.")));
    }

    @Test
    void anExhaustedDailyRouteQuotaReturnsALocalizedTooManyRequests() throws Exception {
        mockMvc.perform(get("/route-quota-exhausted").header("Accept-Language", "de"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", equalTo(
                        "Das Tageslimit für Routenberechnungen ist erreicht. Bitte versuchen Sie es morgen erneut.")));
    }

    @Test
    void disabledRoutingReturnsALocalizedNotImplemented() throws Exception {
        mockMvc.perform(get("/routing-disabled").header("Accept-Language", "en"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message", equalTo("Road routing is not enabled on this server.")));
    }

    @Test
    void anUnknownApartmentReturnsALocalizedNotFound() throws Exception {
        mockMvc.perform(get("/apartment-not-found").header("Accept-Language", "de"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Es existiert keine Wohnung mit der ID apartment-1.")));
    }

    @Test
    void anUnknownTenantReturnsALocalizedNotFound() throws Exception {
        mockMvc.perform(get("/tenant-not-found").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("No tenant exists with id tenant-1.")));
    }

    @Test
    void aReachedApartmentLimitReturnsALocalizedConflict() throws Exception {
        mockMvc.perform(get("/apartment-limit-reached").header("Accept-Language", "de"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo("Ein Objekt kann höchstens 200 Wohnungen haben.")));
    }

    @Test
    void aReachedTenantLimitReturnsALocalizedConflict() throws Exception {
        mockMvc.perform(get("/tenant-limit-reached").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo("An apartment can have at most 10 tenants.")));
    }

    @RestController
    static class FailingController {

        @GetMapping("/apartment-not-found")
        String failWithApartmentNotFound() {
            throw new ApartmentNotFoundException("apartment-1");
        }

        @GetMapping("/tenant-not-found")
        String failWithTenantNotFound() {
            throw new TenantNotFoundException("tenant-1");
        }

        @GetMapping("/apartment-limit-reached")
        String failWithApartmentLimitReached() {
            throw new TenantLimitExceededException(TenantLimitExceededException.REASON_APARTMENT_LIMIT);
        }

        @GetMapping("/tenant-limit-reached")
        String failWithTenantLimitReached() {
            throw new TenantLimitExceededException(TenantLimitExceededException.REASON_TENANT_LIMIT);
        }

        @GetMapping("/route-quota-exhausted")
        String failWithRouteQuotaExhausted() {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_ROUTE_QUOTA_EXHAUSTED);
        }

        @GetMapping("/routing-disabled")
        String failWithRoutingDisabled() {
            throw new RoutingDisabledException();
        }

        @GetMapping("/route-rate-limit-exceeded")
        String failWithRouteRateLimitExceeded() {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_ROUTE_REQUESTS);
        }

        @GetMapping("/routing-unavailable")
        String failWithRoutingUnavailable() {
            throw new RoutingUnavailableException("openrouteservice request failed");
        }

        @GetMapping("/invalid-credentials")
        String failWithInvalidCredentials() {
            throw new InvalidCredentialsException();
        }

        @GetMapping("/account-not-found")
        String failWithAccountNotFound() {
            throw new AccountNotFoundException("deleted-account");
        }

        @GetMapping("/rate-limit-exceeded")
        String failWithRateLimitExceeded() {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_LOGIN_ATTEMPTS);
        }

        @GetMapping("/creation-quota-exceeded")
        String failWithCreationQuotaExceeded() {
            throw new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_PROPERTY);
        }

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
