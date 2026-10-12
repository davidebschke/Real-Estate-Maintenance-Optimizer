package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentYearResponse;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsRequest;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsResponse;
import com.remo.realestatemaintainceoptimizer.dto.TenantRentYearResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentMonthPayment;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the rent payments of an account's tenants: storing confirmed payments, the yearly payment table of a property and the alerts for overdue or awaited rent; another account's properties behave as if they did not exist.
 */
@Service
@Transactional
@EnableConfigurationProperties(PaymentProperties.class)
public class RentPaymentService {

    private static final int MONTHS_PER_YEAR = Month.values().length;

    private static final Comparator<TenantRentYearResponse> TENANT_ORDER = Comparator
            .comparingInt(TenantRentYearResponse::floor)
            .thenComparing(TenantRentYearResponse::lastName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TenantRentYearResponse::firstName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TenantRentYearResponse::tenantId);

    private final PropertyRepository propertyRepository;
    private final ApartmentRepository apartmentRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final RentStatusCalculator statusCalculator;
    private final PaymentProperties properties;

    public RentPaymentService(
            PropertyRepository propertyRepository,
            ApartmentRepository apartmentRepository,
            RentPaymentRepository rentPaymentRepository,
            RentStatusCalculator statusCalculator,
            PaymentProperties properties) {
        this.propertyRepository = propertyRepository;
        this.apartmentRepository = apartmentRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.statusCalculator = statusCalculator;
        this.properties = properties;
    }

    /**
     * Returns the rent payment table of the given property for the given year, or the current year if none is given, with one row per tenant; an apartment's rent is judged by the payments of all its tenants together.
     */
    @Transactional(readOnly = true)
    public RentYearResponse yearOverview(String ownerId, String propertyId, Integer year) {
        loadPropertyOrThrow(ownerId, propertyId);
        LocalDate today = LocalDate.now(properties.zone());
        int resolvedYear = year == null ? today.getYear() : year;
        if (resolvedYear < RentPaymentRequest.FIRST_YEAR || resolvedYear > RentPaymentRequest.LAST_YEAR) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_INVALID_YEAR);
        }
        Map<String, Map<YearMonth, BigDecimal>> paidByApartment =
                paidAmounts(rentPaymentRepository.sumByApartmentAndMonthForProperty(propertyId));
        List<TenantRentYearResponse> tenants = apartmentRepository.findAllByPropertyId(propertyId).stream()
                .flatMap(apartment -> toTenantRows(apartment, paidByApartment, resolvedYear, today))
                .sorted(TENANT_ORDER)
                .toList();
        return new RentYearResponse(resolvedYear, tenants);
    }

    /**
     * Returns one alert for every tenant of the account whose rent is overdue or still awaited, so the frontend can mark the tenant and their property.
     */
    @Transactional(readOnly = true)
    public List<RentAlertResponse> alerts(String ownerId) {
        LocalDate today = LocalDate.now(properties.zone());
        Map<String, Map<YearMonth, BigDecimal>> paidByApartment =
                paidAmounts(rentPaymentRepository.sumByApartmentAndMonthForOwner(ownerId));
        return apartmentRepository.findAllByPropertyOwnerId(ownerId).stream()
                .flatMap(apartment -> {
                    RentAlertLevel alert = statusCalculator.alertLevel(
                            apartment.totalRent(), paidByApartment.getOrDefault(apartment.id(), Map.of()), today);
                    return alert == RentAlertLevel.NONE
                            ? Stream.<RentAlertResponse>empty()
                            : apartment.tenants().stream().map(tenant -> new RentAlertResponse(
                                    apartment.property().id(), apartment.id(), tenant.id(), alert));
                })
                .toList();
    }

    /**
     * Stores the confirmed payments for tenants of the given property and skips every payment that already exists with the same date, amount and reference; all tenants are checked first, so one unknown tenant stores nothing.
     */
    public SaveRentPaymentsResponse save(String ownerId, String propertyId, SaveRentPaymentsRequest request) {
        loadPropertyOrThrow(ownerId, propertyId);
        Map<String, Tenant> tenantsById = apartmentRepository.findAllByPropertyId(propertyId).stream()
                .flatMap(apartment -> apartment.tenants().stream())
                .collect(Collectors.toMap(Tenant::id, Function.identity()));
        for (RentPaymentRequest payment : request.payments()) {
            if (!tenantsById.containsKey(payment.tenantId())) {
                throw new TenantNotFoundException(payment.tenantId());
            }
        }
        int savedCount = 0;
        for (RentPaymentRequest payment : request.payments()) {
            String reference = payment.reference().strip();
            if (rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                    payment.tenantId(), payment.paymentDate(), payment.amount(), reference)) {
                continue;
            }
            rentPaymentRepository.save(new RentPayment(
                    UUID.randomUUID().toString(),
                    tenantsById.get(payment.tenantId()),
                    payment.paymentDate(),
                    payment.amount(),
                    reference,
                    payment.periodYear(),
                    payment.periodMonth()));
            savedCount++;
        }
        return new SaveRentPaymentsResponse(savedCount, request.payments().size() - savedCount);
    }

    private Stream<TenantRentYearResponse> toTenantRows(
            Apartment apartment, Map<String, Map<YearMonth, BigDecimal>> paidByApartment, int year, LocalDate today) {
        Map<YearMonth, BigDecimal> paidByMonth = paidByApartment.getOrDefault(apartment.id(), Map.of());
        YearMonth trackingStart = statusCalculator.trackingStart(paidByMonth.keySet(), today);
        List<RentMonthResponse> months = IntStream.rangeClosed(1, MONTHS_PER_YEAR)
                .mapToObj(month -> {
                    YearMonth yearMonth = YearMonth.of(year, month);
                    BigDecimal paid = paidByMonth.getOrDefault(yearMonth, BigDecimal.ZERO);
                    return new RentMonthResponse(
                            month, statusCalculator.monthStatus(yearMonth, apartment.totalRent(), paid, trackingStart, today), paid);
                })
                .toList();
        RentAlertLevel alert = statusCalculator.alertLevel(apartment.totalRent(), paidByMonth, today);
        return apartment.tenants().stream().map(tenant -> new TenantRentYearResponse(
                tenant.id(),
                apartment.id(),
                tenant.firstName(),
                tenant.lastName(),
                apartment.floor(),
                apartment.totalRent(),
                alert,
                months));
    }

    private static Map<String, Map<YearMonth, BigDecimal>> paidAmounts(List<ApartmentMonthPayment> sums) {
        return sums.stream().collect(Collectors.groupingBy(
                ApartmentMonthPayment::getApartmentId,
                Collectors.toMap(sum -> YearMonth.of(sum.getPeriodYear(), sum.getPeriodMonth()), ApartmentMonthPayment::getTotalAmount)));
    }

    private void loadPropertyOrThrow(String ownerId, String propertyId) {
        propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }
}
