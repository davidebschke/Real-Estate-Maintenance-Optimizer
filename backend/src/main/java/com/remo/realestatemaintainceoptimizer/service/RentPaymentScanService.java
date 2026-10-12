package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor;
import com.remo.realestatemaintainceoptimizer.ai.RentPaymentAdvisor.ExtractedPayment;
import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentScanResponse;
import com.remo.realestatemaintainceoptimizer.dto.ScannedPaymentResponse;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RentStatementTooLargeException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import com.remo.realestatemaintainceoptimizer.service.TenantNameMatcher.Assignment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Scans an uploaded bank statement for the rent payments of a property's tenants: the PDF only lives in memory for the duration of the request, Claude reads the payments off it outside any database transaction and the backend itself assigns them to tenants, leaving it to the user to confirm the result.
 */
@Service
@EnableConfigurationProperties(PaymentProperties.class)
public class RentPaymentScanService {

    static final Duration ACCOUNT_LIMIT_WINDOW = Duration.ofHours(1);
    static final Duration DAILY_LIMIT_WINDOW = Duration.ofDays(1);
    private static final String ALL_ACCOUNTS_KEY = "all-accounts";
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final RentPaymentAdvisor advisor;
    private final TenantNameMatcher nameMatcher;
    private final PropertyRepository propertyRepository;
    private final ApartmentRepository apartmentRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final PaymentProperties properties;
    private final TransactionTemplate readOnlyTransaction;
    private final SlidingWindowRateLimiter scansPerAccount;
    private final SlidingWindowRateLimiter scansPerDay;

    public RentPaymentScanService(
            RentPaymentAdvisor advisor,
            TenantNameMatcher nameMatcher,
            PropertyRepository propertyRepository,
            ApartmentRepository apartmentRepository,
            RentPaymentRepository rentPaymentRepository,
            PaymentProperties properties,
            PlatformTransactionManager transactionManager) {
        this.advisor = advisor;
        this.nameMatcher = nameMatcher;
        this.propertyRepository = propertyRepository;
        this.apartmentRepository = apartmentRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.properties = properties;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.scansPerAccount = new SlidingWindowRateLimiter(
                properties.maxScansPerAccountPerHour(), ACCOUNT_LIMIT_WINDOW, Clock.systemUTC());
        this.scansPerDay = new SlidingWindowRateLimiter(properties.maxScansPerDay(), DAILY_LIMIT_WINDOW, Clock.systemUTC());
    }

    /**
     * Reads the payments off the given statement and assigns each to a tenant of the given property, rejecting a switched-off AI, an unknown property, an empty, oversized or non-PDF file and an exceeded scan limit before the AI is consulted.
     */
    public RentPaymentScanResponse scan(String ownerId, String propertyId, MultipartFile statement) {
        advisor.requireEnabled();
        readOnlyTransaction.executeWithoutResult(status -> requireProperty(ownerId, propertyId));
        byte[] statementPdf = readStatement(statement);
        acquireScanBudget(ownerId);
        List<ExtractedPayment> extracted = advisor.extractPayments(statementPdf);
        return readOnlyTransaction.execute(status -> assignToTenants(propertyId, extracted));
    }

    private void requireProperty(String ownerId, String propertyId) {
        propertyRepository.findByIdAndOwnerId(propertyId, ownerId).orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }

    private byte[] readStatement(MultipartFile statement) {
        if (statement.isEmpty()) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_STATEMENT_EMPTY);
        }
        if (statement.getSize() > properties.maxStatementSize().toBytes()) {
            throw new RentStatementTooLargeException(properties.maxStatementSize().toBytes());
        }
        byte[] bytes;
        try {
            bytes = statement.getBytes();
        } catch (IOException exception) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_STATEMENT_UNREADABLE, exception);
        }
        if (!startsWithPdfSignature(bytes)) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_STATEMENT_NOT_PDF);
        }
        return bytes;
    }

    private static boolean startsWithPdfSignature(byte[] bytes) {
        if (bytes.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PDF_SIGNATURE.length; index++) {
            if (bytes[index] != PDF_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private void acquireScanBudget(String ownerId) {
        if (!scansPerAccount.tryAcquire(ownerId)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_PAYMENT_SCANS);
        }
        if (!scansPerDay.tryAcquire(ALL_ACCOUNTS_KEY)) {
            scansPerAccount.releaseLatest(ownerId);
            throw new RateLimitExceededException(RateLimitExceededException.REASON_PAYMENT_SCAN_QUOTA_EXHAUSTED);
        }
    }

    private RentPaymentScanResponse assignToTenants(String propertyId, List<ExtractedPayment> extracted) {
        List<Tenant> tenants = apartmentRepository.findAllByPropertyId(propertyId).stream()
                .flatMap(apartment -> apartment.tenants().stream())
                .toList();
        List<ScannedPaymentResponse> payments = extracted.stream()
                .map(payment -> toScannedPayment(payment, nameMatcher.assign(payment.payerName(), tenants)))
                .toList();
        return new RentPaymentScanResponse(payments);
    }

    private ScannedPaymentResponse toScannedPayment(ExtractedPayment payment, Assignment assignment) {
        Tenant tenant = assignment.tenant();
        boolean alreadyRecorded = tenant != null && rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                tenant.id(), payment.paymentDate(), payment.amount(), payment.reference());
        return new ScannedPaymentResponse(
                payment.paymentDate(),
                payment.amount(),
                payment.payerName(),
                payment.reference(),
                payment.periodYear(),
                payment.periodMonth(),
                tenant == null ? null : tenant.id(),
                assignment.match(),
                alreadyRecorded);
    }
}
