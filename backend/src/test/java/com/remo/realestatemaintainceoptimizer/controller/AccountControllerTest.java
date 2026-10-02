package com.remo.realestatemaintainceoptimizer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SessionCookieManager;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies changing the username, password and appointment buffer over HTTP, including session handling, demo account rejection and the localized error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountControllerTest {

    private static final String PASSWORD = "correct horse battery staple";
    private static final String NEW_PASSWORD = "another long passphrase";

    @Autowired
    private MockMvc unauthenticatedMockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User account;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        account = TestAccounts.saveRegularAccount(userRepository, passwordEncoder.encode(PASSWORD));
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, account);
    }

    @Test
    void changingTheUsernameStoresItNormalizedAndReturnsTheUpdatedAccount() throws Exception {
        putJson("/api/account/username", "{\"username\":\"New.Name_1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo("new.name_1")));

        assertThat(userRepository.findById(account.id()).orElseThrow().username()).isEqualTo("new.name_1");
    }

    @Test
    void theSessionStaysValidAfterChangingTheUsername() throws Exception {
        putJson("/api/account/username", "{\"username\":\"renamed-user\"}").andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo("renamed-user")));
    }

    @Test
    void aUsernameOfAnotherAccountIsRejectedWithALocalizedConflict() throws Exception {
        User other = TestAccounts.saveRegularAccount(userRepository);

        putJson("/api/account/username", "{\"username\":\"" + other.username() + "\"}", "de")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo("Dieser Benutzername ist bereits vergeben.")));
    }

    @Test
    void keepingTheCurrentUsernameSucceeds() throws Exception {
        putJson("/api/account/username", "{\"username\":\"" + account.username().toUpperCase() + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(account.username())));
    }

    @Test
    void invalidUsernamesAreRejected() throws Exception {
        for (String invalid : new String[] {"ab", "with space", "ümlaut", "", "a".repeat(51), "demo-abc", "DEMO-abc"}) {
            putJson("/api/account/username", "{\"username\":\"" + invalid + "\"}").andExpect(status().isBadRequest());
        }
    }

    @Test
    void changingThePasswordWithTheCorrectCurrentPasswordStartsAFreshSessionAndEndsTheOldOne() throws Exception {
        MvcResult result = putJson("/api/account/password", passwordBody(PASSWORD, NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(account.username())))
                .andReturn();
        Cookie freshSession = result.getResponse().getCookie(SessionCookieManager.COOKIE_NAME);

        assertThat(freshSession).isNotNull();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, userRepository.findById(account.id()).orElseThrow().passwordHash()))
                .isTrue();
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        unauthenticatedMockMvc.perform(get("/api/auth/me").cookie(freshSession)).andExpect(status().isOk());
    }

    @Test
    void aWrongCurrentPasswordIsRejectedWithALocalizedErrorAndKeepsThePassword() throws Exception {
        putJson("/api/account/password", passwordBody("wrong password", NEW_PASSWORD), "de")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message", equalTo("Das aktuelle Passwort ist falsch.")));

        assertThat(passwordEncoder.matches(PASSWORD, userRepository.findById(account.id()).orElseThrow().passwordHash()))
                .isTrue();
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isOk());
    }

    @Test
    void aTooShortNewPasswordIsRejected() throws Exception {
        putJson("/api/account/password", passwordBody(PASSWORD, "short")).andExpect(status().isBadRequest());
    }

    @Test
    void aNewPasswordLongerThanBcryptSupportsIsRejectedWithALocalizedError() throws Exception {
        putJson("/api/account/password", passwordBody(PASSWORD, "ä".repeat(40)), "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("The new password is too long.")));
    }

    @Test
    void aNewPasswordEqualToTheCurrentOneIsRejected() throws Exception {
        putJson("/api/account/password", passwordBody(PASSWORD, PASSWORD), "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("The new password must differ from the current one.")));
    }

    @Test
    void tooManyWrongCurrentPasswordsAreRejectedWithTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            putJson("/api/account/password", passwordBody("wrong password", NEW_PASSWORD))
                    .andExpect(status().isUnprocessableContent());
        }

        putJson("/api/account/password", passwordBody(PASSWORD, NEW_PASSWORD), "en")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", equalTo("Too many failed password changes. Please try again in a few minutes.")));
    }

    @Test
    void changingTheAppointmentBufferReturnsAndPersistsTheNewValue() throws Exception {
        putJson("/api/account/appointment-buffer", "{\"appointmentBufferMinutes\":45}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentBufferMinutes", equalTo(45)));

        assertThat(userRepository.findById(account.id()).orElseThrow().appointmentBufferMinutes()).isEqualTo(45);
        mockMvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.appointmentBufferMinutes", equalTo(45)));
    }

    @Test
    void aZeroBufferIsAllowed() throws Exception {
        putJson("/api/account/appointment-buffer", "{\"appointmentBufferMinutes\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentBufferMinutes", equalTo(0)));
    }

    @Test
    void anAccountWithoutItsOwnBufferReportsTheConfiguredDefault() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.appointmentBufferMinutes", equalTo(15)));
    }

    @Test
    void anOutOfRangeOrMissingBufferIsRejected() throws Exception {
        putJson("/api/account/appointment-buffer", "{\"appointmentBufferMinutes\":-1}").andExpect(status().isBadRequest());
        putJson("/api/account/appointment-buffer", "{\"appointmentBufferMinutes\":1441}").andExpect(status().isBadRequest());
        putJson("/api/account/appointment-buffer", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void demoAccountsCannotChangeAnyProfileSetting() throws Exception {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);
        MockMvc demoMockMvc = TestAccounts.mockMvcAs(context, jwtService, demo);

        demoMockMvc.perform(put("/api/account/username").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"free-name\"}").header("Accept-Language", "de"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("Demo-Accounts können keine Profileinstellungen ändern.")));
        demoMockMvc.perform(put("/api/account/password").contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isForbidden());
        demoMockMvc.perform(put("/api/account/appointment-buffer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentBufferMinutes\":30}"))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(demo.id()).orElseThrow().appointmentBufferMinutes()).isNull();
    }

    @Test
    void everyProfileSettingRequiresASession() throws Exception {
        for (String path : new String[] {"/api/account/username", "/api/account/password", "/api/account/appointment-buffer"}) {
            unauthenticatedMockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{}")
                            .with(TestAccounts.withCsrfToken()))
                    .andExpect(status().isUnauthorized());
        }
    }

    private ResultActions putJson(String path, String body) throws Exception {
        return putJson(path, body, "en");
    }

    private ResultActions putJson(String path, String body, String language) throws Exception {
        return mockMvc.perform(put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", language)
                .content(body));
    }

    private static String passwordBody(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}";
    }
}
