package com.remo.realestatemaintainceoptimizer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor;
import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor.ExtractedPayment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the rent payment REST API through the whole security filter chain against a real PostgreSQL database and a stubbed Claude advisor: scan, confirm, yearly table and alerts, validation, localized errors and account isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RentPaymentControllerTest {

    private static final byte[] PDF = "%PDF-1.7 statement".getBytes(StandardCharsets.US_ASCII);
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Berlin"));
    private static final YearMonth CURRENT_MONTH = YearMonth.from(TODAY);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc anonymousMockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @MockitoBean
    private RentPaymentAdvisor advisor;

    private MockMvc mockMvc;

    private MockMvc otherAccountMockMvc;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        User owner = TestAccounts.saveRegularAccount(userRepository);
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, owner);
        otherAccountMockMvc = TestAccounts.mockMvcAs(context, jwtService, TestAccounts.saveRegularAccount(userRepository));
        Property property = new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building");
        property.activateRentTracking(CURRENT_MONTH);
        property = propertyRepository.save(property);
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-1", "850.00",
                new TenantSpec("tenant-1", "Erika", "Mustermann"));
        Property inactive = propertyRepository.save(
                new Property("property-2", owner.id(), "Mediapark", "Im Mediapark 1", "pi-building"));
        RentFixtures.saveApartment(apartmentRepository, inactive, "apartment-2", "700.00",
                new TenantSpec("tenant-2", "Anna", "Adler"));
    }

    private static MockMultipartFile statement(byte[] content) {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", content);
    }

    private static String confirmBody(String tenantId, String amount, int month) {
        return """
                {"payments":[{"tenantId":"%s","paymentDate":"%s","amount":%s,"reference":"Miete","periodYear":%d,"periodMonth":%d}]}
                """.formatted(tenantId, TODAY, amount, CURRENT_MONTH.getYear(), month);
    }

    @Test
    void refusesEveryRentPaymentRequestWithoutALoggedInAccount() throws Exception {
        anonymousMockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1").file(statement(PDF))
                        .with(TestAccounts.withCsrfToken()))
                .andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(post("/api/properties/{id}/rent-payments", "property-1").with(TestAccounts.withCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody("tenant-1", "850", 1)))
                .andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(get("/api/properties/{id}/rent-payments", "property-1")).andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(put("/api/properties/{id}/rent-payments/tracking", "property-1").with(TestAccounts.withCsrfToken()))
                .andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(get("/api/rent-payments/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void activatesTheCheckOfAPropertyFromTheCurrentMonthOnAndShowsItInTheYearlyTable() throws Exception {
        mockMvc.perform(get("/api/properties/{id}/rent-payments", "property-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStart", equalTo(null)))
                .andExpect(jsonPath("$.formerTenantPayments", hasSize(0)))
                .andExpect(jsonPath("$.tenants[0].alert", equalTo("NONE")))
                .andExpect(jsonPath("$.tenants[0].months[0].status", equalTo("UNTRACKED")));

        mockMvc.perform(put("/api/properties/{id}/rent-payments/tracking", "property-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStart", equalTo(CURRENT_MONTH.toString())));

        mockMvc.perform(get("/api/properties/{id}/rent-payments", "property-2"))
                .andExpect(jsonPath("$.trackingStart", equalTo(CURRENT_MONTH.toString())))
                .andExpect(jsonPath("$.tenants[0].months[%d].expectedAmount".formatted(CURRENT_MONTH.getMonthValue() - 1), equalTo(700.0)));
    }

    @Test
    void scanningAndStoringOnAPropertyWithoutActivatedCheckAnswersConflict() throws Exception {
        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-2").file(statement(PDF)).header("Accept-Language", "de"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", equalTo("Der Mietzahlungscheck ist für dieses Objekt noch nicht aktiviert.")));
        mockMvc.perform(post("/api/properties/{id}/rent-payments", "property-2")
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody("tenant-2", "700", 1)))
                .andExpect(status().isConflict());
        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void anotherAccountCannotActivateTheCheck() throws Exception {
        otherAccountMockMvc.perform(put("/api/properties/{id}/rent-payments/tracking", "property-2")).andExpect(status().isNotFound());
    }

    @Test
    void scansAStatementAndAssignsTheFoundPaymentToATenant() throws Exception {
        when(advisor.extractPayments(PDF)).thenReturn(List.of(new ExtractedPayment(
                LocalDate.of(2026, 10, 2), new BigDecimal("850.00"), "Erika Mustermann", "Miete Oktober 2026", 2026, 10)));

        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1").file(statement(PDF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.payments[0].paymentDate", equalTo("2026-10-02")))
                .andExpect(jsonPath("$.payments[0].amount", equalTo(850.0)))
                .andExpect(jsonPath("$.payments[0].payerName", equalTo("Erika Mustermann")))
                .andExpect(jsonPath("$.payments[0].periodYear", equalTo(2026)))
                .andExpect(jsonPath("$.payments[0].periodMonth", equalTo(10)))
                .andExpect(jsonPath("$.payments[0].tenantId", equalTo("tenant-1")))
                .andExpect(jsonPath("$.payments[0].match", equalTo("MATCH")))
                .andExpect(jsonPath("$.payments[0].alreadyRecorded", equalTo(false)));
        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void rejectsAFileThatIsNotAPdfAndAMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1")
                        .file(statement("plain text".getBytes(StandardCharsets.UTF_8))).header("Accept-Language", "de"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Die hochgeladene Datei ist kein PDF-Dokument.")));
        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersNotImplementedWhileTheAiIsOffAndServiceUnavailableWhenItFails() throws Exception {
        doThrow(new AiDisabledException()).when(advisor).requireEnabled();
        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1").file(statement(PDF)))
                .andExpect(status().isNotImplemented());

        reset(advisor);
        when(advisor.extractPayments(any())).thenThrow(new AiUnavailableException("down"));
        mockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1").file(statement(PDF)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void storesConfirmedPaymentsAndShowsThemInTheYearlyTableAndNoMoreAlert() throws Exception {
        mockMvc.perform(post("/api/properties/{id}/rent-payments", "property-1")
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody("tenant-1", "850.00", CURRENT_MONTH.getMonthValue())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedCount", equalTo(1)))
                .andExpect(jsonPath("$.skippedCount", equalTo(0)));

        mockMvc.perform(get("/api/properties/{id}/rent-payments", "property-1").param("year", String.valueOf(CURRENT_MONTH.getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", equalTo(CURRENT_MONTH.getYear())))
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].tenantId", equalTo("tenant-1")))
                .andExpect(jsonPath("$.tenants[0].monthlyRent", equalTo(850.0)))
                .andExpect(jsonPath("$.tenants[0].alert", equalTo("NONE")))
                .andExpect(jsonPath("$.tenants[0].months", hasSize(12)))
                .andExpect(jsonPath("$.tenants[0].months[%d].status".formatted(CURRENT_MONTH.getMonthValue() - 1), equalTo("PAID")))
                .andExpect(jsonPath("$.tenants[0].months[%d].paidAmount".formatted(CURRENT_MONTH.getMonthValue() - 1), equalTo(850.0)));
        mockMvc.perform(get("/api/rent-payments/alerts")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anUnpaidRentShowsUpAsAnAlertOfTheTenantAndTheirProperty() throws Exception {
        String expectedAlert = TODAY.getDayOfMonth() > 5 ? "OVERDUE" : "PENDING";

        mockMvc.perform(get("/api/rent-payments/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].propertyId", equalTo("property-1")))
                .andExpect(jsonPath("$[0].apartmentId", equalTo("apartment-1")))
                .andExpect(jsonPath("$[0].tenantId", equalTo("tenant-1")))
                .andExpect(jsonPath("$[0].alert", equalTo(expectedAlert)));
    }

    @Test
    void rejectsInvalidPaymentsWithALocalizedBadRequest() throws Exception {
        for (String body : List.of(
                "{\"payments\":[]}",
                confirmBody("tenant-1", "-5", 1),
                confirmBody("tenant-1", "0", 1),
                confirmBody("tenant-1", "850", 13),
                confirmBody("tenant-1", "850", 0),
                "{\"payments\":[{\"paymentDate\":\"2026-10-02\",\"amount\":850,\"reference\":\"x\",\"periodYear\":2026,\"periodMonth\":10}]}",
                "{\"payments\":[null]}",
                "{}")) {
            mockMvc.perform(post("/api/properties/{id}/rent-payments", "property-1")
                            .contentType(MediaType.APPLICATION_JSON).content(body).header("Accept-Language", "en"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", equalTo("The submitted data is invalid.")));
        }
        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void anUnknownTenantAnswersNotFoundAndStoresNothing() throws Exception {
        mockMvc.perform(post("/api/properties/{id}/rent-payments", "property-1")
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody("unknown", "850", 1)))
                .andExpect(status().isNotFound());

        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void rejectsAnUnsupportedYear() throws Exception {
        mockMvc.perform(get("/api/properties/{id}/rent-payments", "property-1").param("year", "1999").header("Accept-Language", "de"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Das Jahr muss zwischen 2000 und 2100 liegen.")));
    }

    @Test
    void anotherAccountSeesNoPropertyAndNoAlerts() throws Exception {
        otherAccountMockMvc.perform(get("/api/properties/{id}/rent-payments", "property-1")).andExpect(status().isNotFound());
        otherAccountMockMvc.perform(post("/api/properties/{id}/rent-payments", "property-1")
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody("tenant-1", "850", 1)))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(multipart("/api/properties/{id}/rent-payments/scan", "property-1").file(statement(PDF)))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(get("/api/rent-payments/alerts")).andExpect(jsonPath("$", hasSize(0)));
    }
}
