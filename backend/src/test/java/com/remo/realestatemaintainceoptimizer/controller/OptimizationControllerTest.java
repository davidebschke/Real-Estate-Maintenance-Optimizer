package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationProposalResponse;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationRunResponse;
import com.remo.realestatemaintainceoptimizer.dto.SavingsGranularity;
import com.remo.realestatemaintainceoptimizer.dto.SavingsPeriodResponse;
import com.remo.realestatemaintainceoptimizer.dto.SavingsStatisticsResponse;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.service.OptimizationProposalService;
import com.remo.realestatemaintainceoptimizer.service.OptimizationSavingsService;
import com.remo.realestatemaintainceoptimizer.service.OptimizationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the optimization REST API against stubbed services: authentication, the account and language handed to the services, the JSON shape of runs, proposals and savings, and the HTTP mapping of failures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OptimizationControllerTest {

    private static final LocalDateTime PROPOSED_START = LocalDateTime.of(2026, 11, 4, 9, 15);
    private static final OptimizationProposalResponse PROPOSAL = new OptimizationProposalResponse(
            "proposal-1", "appointment-1", "Heizungswartung", "Rheinhaus Ost", false,
            LocalDateTime.of(2026, 11, 3, 10, 0), LocalDateTime.of(2026, 11, 3, 11, 0),
            PROPOSED_START, PROPOSED_START.plusHours(1), 10_000.0, 900.0, "Spart Fahrzeit.",
            OptimizationProposalStatus.PENDING, null);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc anonymousMockMvc;

    @MockitoBean
    private OptimizationService optimizationService;

    @MockitoBean
    private OptimizationProposalService proposalService;

    @MockitoBean
    private OptimizationSavingsService savingsService;

    private User account;
    private MockMvc mockMvc;

    @BeforeEach
    void logIn() {
        account = TestAccounts.saveRegularAccount(userRepository);
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, account);
    }

    @Test
    void refusesEveryOptimizationRequestWithoutALoggedInAccount() throws Exception {
        anonymousMockMvc.perform(post("/api/optimizations").with(TestAccounts.withCsrfToken()))
                .andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(get("/api/optimizations/proposals")).andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(get("/api/optimizations/savings")).andExpect(status().isUnauthorized());

        verifyNoInteractions(optimizationService, proposalService, savingsService);
    }

    @Test
    void startsARunForTheLoggedInAccountInTheRequestLanguage() throws Exception {
        when(optimizationService.run(eq(account.id()), eq(Locale.ENGLISH)))
                .thenReturn(new OptimizationRunResponse("run-1", Instant.parse("2026-10-09T08:00:00Z"), 4, 2, List.of(PROPOSAL)));

        mockMvc.perform(post("/api/optimizations").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId", equalTo("run-1")))
                .andExpect(jsonPath("$.analyzedAppointmentCount", equalTo(4)))
                .andExpect(jsonPath("$.candidateCount", equalTo(2)))
                .andExpect(jsonPath("$.proposals", hasSize(1)))
                .andExpect(jsonPath("$.proposals[0].proposedStart", equalTo("2026-11-04T09:15:00")))
                .andExpect(jsonPath("$.proposals[0].savedDistanceMeters", equalTo(10_000.0)))
                .andExpect(jsonPath("$.proposals[0].status", equalTo("PENDING")));
    }

    @Test
    void anExhaustedDemoRunAnswersForbiddenAndADisabledAiNotImplemented() throws Exception {
        when(optimizationService.run(eq(account.id()), eq(Locale.GERMAN)))
                .thenThrow(new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_AI_OPTIMIZATION))
                .thenThrow(new AiDisabledException());

        mockMvc.perform(post("/api/optimizations").header("Accept-Language", "de"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("Ein Demo-Account kann die KI-Optimierung nur einmal nutzen.")));
        mockMvc.perform(post("/api/optimizations").header("Accept-Language", "de"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void listsThePendingProposals() throws Exception {
        when(proposalService.listPending(account.id())).thenReturn(List.of(PROPOSAL));

        mockMvc.perform(get("/api/optimizations/proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo("proposal-1")))
                .andExpect(jsonPath("$[0].reason", equalTo("Spart Fahrzeit.")));
    }

    @Test
    void acceptsAndRejectsAProposalOfTheLoggedInAccount() throws Exception {
        when(proposalService.accept(account.id(), "proposal-1")).thenReturn(PROPOSAL);
        when(proposalService.reject(account.id(), "proposal-2")).thenReturn(PROPOSAL);

        mockMvc.perform(post("/api/optimizations/proposals/proposal-1/accept")).andExpect(status().isOk());
        mockMvc.perform(post("/api/optimizations/proposals/proposal-2/reject")).andExpect(status().isOk());

        verify(proposalService).accept(account.id(), "proposal-1");
        verify(proposalService).reject(account.id(), "proposal-2");
    }

    @Test
    void returnsTheWeeklySavingsByDefaultAndTheMonthlyOnesOnRequest() throws Exception {
        SavingsStatisticsResponse weekly = new SavingsStatisticsResponse(SavingsGranularity.WEEK, 12_500, 1_020, 3,
                List.of(new SavingsPeriodResponse(LocalDate.of(2026, 9, 28), 10_000, 900, 2)));
        when(savingsService.statistics(account.id(), SavingsGranularity.WEEK)).thenReturn(weekly);
        when(savingsService.statistics(account.id(), SavingsGranularity.MONTH))
                .thenReturn(new SavingsStatisticsResponse(SavingsGranularity.MONTH, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/optimizations/savings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity", equalTo("WEEK")))
                .andExpect(jsonPath("$.totalSavedDistanceMeters", equalTo(12_500.0)))
                .andExpect(jsonPath("$.periods[0].periodStart", equalTo("2026-09-28")))
                .andExpect(jsonPath("$.periods[0].acceptedProposalCount", equalTo(2)));
        mockMvc.perform(get("/api/optimizations/savings").param("granularity", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity", equalTo("MONTH")));
    }

    @Test
    void rejectsAnUnknownGranularity() throws Exception {
        mockMvc.perform(get("/api/optimizations/savings").param("granularity", "DAY")).andExpect(status().isBadRequest());

        verifyNoInteractions(savingsService);
    }
}
