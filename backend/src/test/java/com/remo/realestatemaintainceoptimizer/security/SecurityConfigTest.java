package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the security filter chain end to end: which endpoints are public, that everything else needs a valid session, and that state-changing requests need the CSRF token the single-page frontend reads from its cookie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

    private static final String PROPERTY_REQUEST_BODY = """
            {
              "name": "Wohnanlage Nordpark",
              "address": "Nordparkstr. 3, 50733 Köln"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private User account;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        account = TestAccounts.saveRegularAccount(userRepository);
    }

    @Test
    void theVersionEndpointStaysPublicForHealthChecks() throws Exception {
        mockMvc.perform(get("/api/version")).andExpect(status().isOk());
    }

    @Test
    void everyDataEndpointRequiresASession() throws Exception {
        mockMvc.perform(get("/api/properties")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aValidSessionCookieGrantsAccess() throws Exception {
        mockMvc.perform(get("/api/properties").cookie(TestAccounts.sessionCookie(jwtService, account)))
                .andExpect(status().isOk());
    }

    @Test
    void aForgedSessionCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/properties").cookie(new Cookie(SessionCookieManager.COOKIE_NAME, "forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aSessionOfADeletedAccountIsRejected() throws Exception {
        Cookie sessionCookie = TestAccounts.sessionCookie(jwtService, account);
        userRepository.deleteById(account.id());

        mockMvc.perform(get("/api/properties").cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void aSessionOfAnExpiredDemoAccountIsRejected() throws Exception {
        User expiredDemo = TestAccounts.saveDemoAccount(userRepository, Instant.now().minusSeconds(1), 3, 3);

        mockMvc.perform(get("/api/properties").cookie(TestAccounts.sessionCookie(jwtService, expiredDemo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aStateChangingRequestWithoutCsrfTokenIsForbiddenEvenWithAValidSession() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .cookie(TestAccounts.sessionCookie(jwtService, account))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPERTY_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void aStateChangingRequestWithAWrongCsrfTokenIsForbidden() throws Exception {
        String csrfToken = fetchCsrfCookie().getValue();

        mockMvc.perform(post("/api/properties")
                        .cookie(TestAccounts.sessionCookie(jwtService, account), new Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken + "-tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPERTY_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void theCsrfTokenFromTheCookieEchoedInTheHeaderIsAccepted() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/properties")
                        .cookie(TestAccounts.sessionCookie(jwtService, account), csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPERTY_REQUEST_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void everyResponseProvidesAReadableStrictCsrfCookie() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        assertThat(csrfCookie.getValue()).isNotBlank();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(csrfCookie.getSecure()).isTrue();
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Strict");
    }

    @Test
    void theLoginEndpointIsReachableWithoutSessionButStillNeedsTheCsrfToken() throws Exception {
        String requestBody = "{\"username\": \"nobody\", \"password\": \"whatever\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/login")
                        .with(TestAccounts.withCsrfToken())
                        .with(TestAccounts.fromClient(TestAccounts.uniqueClientAddress()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    private Cookie fetchCsrfCookie() throws Exception {
        return mockMvc.perform(get("/api/version")).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
