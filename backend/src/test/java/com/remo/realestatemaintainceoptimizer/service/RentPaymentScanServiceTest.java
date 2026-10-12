package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor;
import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor.ExtractedPayment;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentScanResponse;
import com.remo.realestatemaintainceoptimizer.dto.ScannedPaymentResponse;
import com.remo.realestatemaintainceoptimizer.dto.TenantMatch;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RentStatementTooLargeException;
import com.remo.realestatemaintainceoptimizer.exception.RentTrackingNotActiveException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the statement scan (sharing the Spring context of the controller test via {@code @AutoConfigureMockMvc}) against a real PostgreSQL database and a stubbed Claude advisor: the checks before the AI is consulted (switched off, unknown property, empty, oversized or non-PDF file, scan limit), the assignment of payments to tenants, the duplicate flag and that AI failures propagate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RentPaymentScanServiceTest {

    private static final byte[] PDF = "%PDF-1.7 statement".getBytes(StandardCharsets.US_ASCII);
    private static final LocalDate DATE = LocalDate.of(2026, 10, 2);

    @Autowired
    private RentPaymentScanService service;

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private RentPaymentAdvisor advisor;

    private User owner;

    private Property property;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        property = new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building");
        property.activateRentTracking(YearMonth.of(2026, 1));
        property = propertyRepository.save(property);
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-1", "850.00",
                new TenantSpec("tenant-1", "Erika", "Mustermann"));
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-2", "700.00",
                new TenantSpec("tenant-2", "Anna", "Adler"));
    }

    private static MockMultipartFile statement(byte[] content) {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", content);
    }

    private static ExtractedPayment extracted(String payerName, String amount) {
        return new ExtractedPayment(DATE, new BigDecimal(amount), payerName, "Miete Oktober 2026", 2026, 10);
    }

    @Test
    void assignsTheFoundPaymentsToTenantsAndKeepsTheUnassignedOnes() {
        when(advisor.extractPayments(PDF)).thenReturn(List.of(
                extracted("Erika Mustermann", "850.00"), extracted("Familie Adler", "700.00"), extracted("Stadtwerke", "50.00")));

        RentPaymentScanResponse response = service.scan(owner.id(), "property-1", statement(PDF));

        assertThat(response.payments()).extracting(ScannedPaymentResponse::tenantId, ScannedPaymentResponse::match)
                .containsExactly(
                        tuple("tenant-1", TenantMatch.MATCH),
                        tuple("tenant-2", TenantMatch.POSSIBLE),
                        tuple(null, TenantMatch.NONE));
        assertThat(response.payments().getFirst()).satisfies(payment -> {
            assertThat(payment.paymentDate()).isEqualTo(DATE);
            assertThat(payment.amount()).isEqualByComparingTo("850.00");
            assertThat(payment.payerName()).isEqualTo("Erika Mustermann");
            assertThat(payment.reference()).isEqualTo("Miete Oktober 2026");
            assertThat(payment.periodYear()).isEqualTo(2026);
            assertThat(payment.periodMonth()).isEqualTo(10);
        });
    }

    @Test
    void doesNotStoreAnythingWhileScanning() {
        when(advisor.extractPayments(PDF)).thenReturn(List.of(extracted("Erika Mustermann", "850.00")));

        service.scan(owner.id(), "property-1", statement(PDF));

        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void flagsAPaymentThatIsAlreadyRecorded() {
        Tenant erika = apartmentRepository.findAllByPropertyId("property-1").stream()
                .flatMap(apartment -> apartment.tenants().stream()).filter(tenant -> tenant.id().equals("tenant-1")).findFirst().orElseThrow();
        rentPaymentRepository.save(new RentPayment("payment-1", erika.apartment(), erika, DATE, new BigDecimal("850.00"), "Miete Oktober 2026", 2026, 10));
        when(advisor.extractPayments(PDF)).thenReturn(List.of(
                extracted("Erika Mustermann", "850.00"), extracted("Erika Mustermann", "851.00")));

        RentPaymentScanResponse response = service.scan(owner.id(), "property-1", statement(PDF));

        assertThat(response.payments()).extracting(ScannedPaymentResponse::alreadyRecorded).containsExactly(true, false);
    }

    @Test
    void anEmptyAnswerGivesAnEmptyResult() {
        when(advisor.extractPayments(PDF)).thenReturn(List.of());

        assertThat(service.scan(owner.id(), "property-1", statement(PDF)).payments()).isEmpty();
    }

    @Test
    void aSwitchedOffAiRejectsTheScanBeforeAnythingElse() {
        doThrow(new AiDisabledException()).when(advisor).requireEnabled();

        assertThatThrownBy(() -> service.scan(owner.id(), "unknown", statement(PDF))).isInstanceOf(AiDisabledException.class);

        verify(advisor, never()).extractPayments(any());
    }

    @Test
    void aPropertyWithoutActivatedRentCheckIsRejectedWithoutConsultingTheAi() {
        propertyRepository.save(new Property("property-2", owner.id(), "Mediapark", "Im Mediapark 1", "pi-building"));

        assertThatThrownBy(() -> service.scan(owner.id(), "property-2", statement(PDF)))
                .isInstanceOf(RentTrackingNotActiveException.class);

        verify(advisor, never()).extractPayments(any());
    }

    @Test
    void anotherAccountsPropertyIsRejectedWithoutConsultingTheAi() {
        User stranger = TestAccounts.saveRegularAccount(userRepository);

        assertThatThrownBy(() -> service.scan(stranger.id(), "property-1", statement(PDF)))
                .isInstanceOf(PropertyNotFoundException.class);

        verify(advisor, never()).extractPayments(any());
    }

    @Test
    void anEmptyFileANonPdfFileAndAnOversizedFileAreRejectedWithoutConsultingTheAi() {
        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement(new byte[0])))
                .isInstanceOfSatisfying(InvalidRentRequestException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidRentRequestException.REASON_STATEMENT_EMPTY));
        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement("hello world".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOfSatisfying(InvalidRentRequestException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidRentRequestException.REASON_STATEMENT_NOT_PDF));
        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement(new byte[]{'%'})))
                .isInstanceOf(InvalidRentRequestException.class);
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, tooLarge, 0, PDF.length);
        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement(tooLarge)))
                .isInstanceOf(RentStatementTooLargeException.class);

        verify(advisor, never()).extractPayments(any());
    }

    @Test
    void anAiFailureIsPassedOnToTheCaller() {
        when(advisor.extractPayments(PDF)).thenThrow(new AiUnavailableException("down"));

        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement(PDF))).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void anAccountCanScanOnlyAFewStatementsPerHour() {
        when(advisor.extractPayments(PDF)).thenReturn(List.of());
        for (int scan = 0; scan < 10; scan++) {
            service.scan(owner.id(), "property-1", statement(PDF));
        }

        assertThatThrownBy(() -> service.scan(owner.id(), "property-1", statement(PDF)))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception ->
                        assertThat(exception.reasonCode()).isEqualTo(RateLimitExceededException.REASON_TOO_MANY_PAYMENT_SCANS));
    }
}
