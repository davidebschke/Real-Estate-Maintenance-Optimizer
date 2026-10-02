package com.remo.realestatemaintainceoptimizer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.SessionCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Verifies login, demo account creation, the current-account lookup and logout over HTTP, including the hardened session cookie and the error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User account;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        account = TestAccounts.saveRegularAccount(userRepository, passwordEncoder.encode(PASSWORD));
    }

    @Test
    void aSuccessfulLoginReturnsTheAccountAndSetsAHardenedSessionCookie() throws Exception {
        MvcResult result = login(account.username(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(account.username())))
                .andExpect(jsonPath("$.displayName", equalTo("Test User")))
                .andExpect(jsonPath("$.demoAccount", equalTo(false)))
                .andExpect(jsonPath("$.remainingPropertyCreations", nullValue()))
                .andExpect(jsonPath("$.appointmentBufferMinutes", equalTo(15)))
                .andReturn();

        String setCookie = sessionSetCookieHeader(result);
        assertThat(setCookie)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/api")
                .contains("Max-Age=28800");
    }

    @Test
    void theSessionCookieOfALoginIdentifiesTheAccount() throws Exception {
        Cookie sessionCookie = login(account.username(), PASSWORD).andReturn().getResponse()
                .getCookie(SessionCookieManager.COOKIE_NAME);

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(account.username())))
                .andExpect(jsonPath("$.displayName", equalTo("Test User")));
    }

    @Test
    void aWrongPasswordReturnsALocalizedUnauthorizedWithoutSessionCookie() throws Exception {
        MvcResult result = login(account.username(), "wrong password", "de")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Benutzername oder Passwort ist falsch.")))
                .andReturn();

        assertThat(result.getResponse().getCookie(SessionCookieManager.COOKIE_NAME)).isNull();
    }

    @Test
    void blankCredentialsAreRejectedAsInvalidInput() throws Exception {
        login("", "", "en").andExpect(status().isBadRequest());
    }

    @Test
    void tooManyFailedLoginsReturnALocalizedTooManyRequests() throws Exception {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 5; attempt++) {
            login(account.username(), "wrong password", "en", clientAddress).andExpect(status().isUnauthorized());
        }

        login(account.username(), PASSWORD, "en", clientAddress)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", equalTo(
                        "Too many failed login attempts. Please try again in a few minutes.")));
    }

    @Test
    void creatingADemoAccountStartsASessionWithItsExampleDataAndLimits() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/demo-account")
                        .with(TestAccounts.withCsrfToken())
                        .with(TestAccounts.fromClient(TestAccounts.uniqueClientAddress())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.demoAccount", equalTo(true)))
                .andExpect(jsonPath("$.remainingPropertyCreations", equalTo(3)))
                .andExpect(jsonPath("$.remainingAppointmentCreations", equalTo(3)))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();
        Cookie sessionCookie = result.getResponse().getCookie(SessionCookieManager.COOKIE_NAME);

        mockMvc.perform(get("/api/properties").cookie(sessionCookie)).andExpect(jsonPath("$", hasSize(5)));
        mockMvc.perform(get("/api/appointments").cookie(sessionCookie)).andExpect(jsonPath("$", hasSize(30)));
    }

    @Test
    void tooManyDemoAccountsFromOneClientReturnTooManyRequests() throws Exception {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int index = 0; index < 10; index++) {
            mockMvc.perform(post("/api/auth/demo-account")
                            .with(TestAccounts.withCsrfToken())
                            .with(TestAccounts.fromClient(clientAddress)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/auth/demo-account")
                        .with(TestAccounts.withCsrfToken())
                        .with(TestAccounts.fromClient(clientAddress)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void loggingOutADemoAccountClearsTheCookieAndDeletesTheAccount() throws Exception {
        Cookie sessionCookie = mockMvc.perform(post("/api/auth/demo-account")
                        .with(TestAccounts.withCsrfToken())
                        .with(TestAccounts.fromClient(TestAccounts.uniqueClientAddress())))
                .andReturn().getResponse().getCookie(SessionCookieManager.COOKIE_NAME);

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessionCookie)
                        .with(TestAccounts.withCsrfToken()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(sessionSetCookieHeader(logoutResult)).contains("Max-Age=0");
        assertThat(userRepository.countByDemoAccountTrue()).isZero();
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void loggingOutARegularAccountRevokesTheSessionButKeepsTheAccount() throws Exception {
        Cookie sessionCookie = login(account.username(), PASSWORD).andReturn().getResponse()
                .getCookie(SessionCookieManager.COOKIE_NAME);

        mockMvc.perform(post("/api/auth/logout").cookie(sessionCookie).with(TestAccounts.withCsrfToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie)).andExpect(status().isUnauthorized());
        assertThat(userRepository.findById(account.id())).isPresent();
        Thread.sleep(2);
        Cookie newSessionCookie = login(account.username(), PASSWORD).andReturn().getResponse()
                .getCookie(SessionCookieManager.COOKIE_NAME);
        mockMvc.perform(get("/api/auth/me").cookie(newSessionCookie)).andExpect(status().isOk());
    }

    @Test
    void loggingOutWithoutSessionStillSucceedsAndClearsTheCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout").with(TestAccounts.withCsrfToken()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(sessionSetCookieHeader(result)).contains("Max-Age=0");
    }

    @Test
    void theCurrentAccountRequiresASession() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String username, String password) throws Exception {
        return login(username, password, "en");
    }

    private ResultActions login(String username, String password, String language) throws Exception {
        return login(username, password, language, TestAccounts.uniqueClientAddress());
    }

    private ResultActions login(String username, String password, String language, String clientAddress)
            throws Exception {
        String requestBody = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        return mockMvc.perform(post("/api/auth/login")
                .with(TestAccounts.withCsrfToken())
                .with(TestAccounts.fromClient(clientAddress))
                .header("Accept-Language", language)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private static String sessionSetCookieHeader(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(SessionCookieManager.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();
    }
}
