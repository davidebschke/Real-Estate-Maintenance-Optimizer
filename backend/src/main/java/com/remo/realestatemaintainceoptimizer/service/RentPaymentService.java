package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.FormerTenantPaymentResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentTrackingResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentYearResponse;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsRequest;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsResponse;
import com.remo.realestatemaintainceoptimizer.dto.TenantRentYearResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RentTrackingNotActiveException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentMonthPayment;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRentChangeRepository;
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
 * Business logic for the rent payments of an account's tenants: activating the rent payment check of a property, storing confirmed payments, the yearly payment table of a property and the alerts for overdue or awaited rent; another account's properties behave as if they did not exist.
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
    private final ApartmentRentChangeRepository rentChangeRepository;
    private final RentStatusCalculator statusCalculator;
    private final PaymentProperties properties;

    public RentPaymentService(
            PropertyRepository propertyRepository,
            ApartmentRepository apartmentRepository,
            RentPaymentRepository rentPaymentRepository,
            ApartmentRentChangeRepository rentChangeRepository,
            RentStatusCalculator statusCalculator,
            PaymentProperties properties) {
        this.propertyRepository = propertyRepository;
        this.apartmentRepository = apartmentRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.rentChangeRepository = rentChangeRepository;
        this.statusCalculator = statusCalculator;
        this.properties = properties;
    }

    /**
     * Activates the rent payment check of the given property from the current month on, which counts from the first day of that month, and keeps an earlier start if it was activated before.
     */
    public RentTrackingResponse activateTracking(String ownerId, String propertyId) {
        Property property = loadPropertyOrThrow(ownerId, propertyId);
        property.activateRentTracking(YearMonth.now(properties.zone()));
        return new RentTrackingResponse(property.rentTrackingStart().toString());
    }

    /**
     * Returns the rent payment table of the given property for the given year, or the current year if none is given, with one row per tenant and the payments of former tenants; an apartment's rent is judged by the payments of all its tenants together and, month by month, by the rent due in that month, and nothing is tracked while the check is not activated.
     */
    @Transactional(readOnly = true)
    public RentYearResponse yearOverview(String ownerId, String propertyId, Integer year) {
        Property property = loadPropertyOrThrow(ownerId, propertyId);
        LocalDate today = LocalDate.now(properties.zone());
        int resolvedYear = year == null ? today.getYear() : year;
        if (resolvedYear < RentPaymentRequest.FIRST_YEAR || resolvedYear > RentPaymentRequest.LAST_YEAR) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_INVALID_YEAR);
        }
        YearMonth trackingStart = property.rentTrackingStart();
        Map<String, Map<YearMonth, BigDecimal>> paidByApartment =
                paidAmounts(rentPaymentRepository.sumByApartmentAndMonthForProperty(propertyId));
        Map<String, List<ApartmentRentChange>> changesByApartment = changesByApartment(
                rentChangeRepository.findAllByApartmentPropertyId(propertyId));
        List<TenantRentYearResponse> tenants = apartmentRepository.findAllByPropertyId(propertyId).stream()
                .flatMap(apartment -> toTenantRows(
                        apartment,
                        RentSchedule.of(apartment.totalRent(), changesByApartment.getOrDefault(apartment.id(), List.of())),
                        paidByApartment.getOrDefault(apartment.id(), Map.of()),
                        new YearView(resolvedYear, trackingStart, today)))
                .sorted(TENANT_ORDER)
                .toList();
        List<FormerTenantPaymentResponse> formerTenantPayments =
                rentPaymentRepository.findFormerTenantPayments(propertyId, resolvedYear).stream()
                        .map(RentPaymentService::toFormerTenantPayment)
                        .toList();
        return new RentYearResponse(
                resolvedYear, trackingStart == null ? null : trackingStart.toString(), tenants, formerTenantPayments);
    }

    /**
     * Returns one alert for every tenant of an account's activated properties whose rent is overdue or still awaited, so the frontend can mark the tenant and their property.
     */
    @Transactional(readOnly = true)
    public List<RentAlertResponse> alerts(String ownerId) {
        LocalDate today = LocalDate.now(properties.zone());
        Map<String, Map<YearMonth, BigDecimal>> paidByApartment =
                paidAmounts(rentPaymentRepository.sumByApartmentAndMonthForOwner(ownerId));
        Map<String, List<ApartmentRentChange>> changesByApartment = changesByApartment(
                rentChangeRepository.findAllByApartmentPropertyOwnerId(ownerId));
        return apartmentRepository.findAllByPropertyOwnerId(ownerId).stream()
                .filter(apartment -> apartment.property().rentTrackingStart() != null)
                .flatMap(apartment -> {
                    RentAlertLevel alert = statusCalculator.alertLevel(
                            RentSchedule.of(apartment.totalRent(), changesByApartment.getOrDefault(apartment.id(), List.of())),
                            paidByApartment.getOrDefault(apartment.id(), Map.of()),
                            apartment.property().rentTrackingStart(),
                            today);
                    return alert == RentAlertLevel.NONE
                            ? Stream.<RentAlertResponse>empty()
                            : apartment.tenants().stream().map(tenant -> new RentAlertResponse(
                                    apartment.property().id(), apartment.id(), tenant.id(), alert));
                })
                .toList();
    }

    /**
     * Stores the confirmed payments for tenants of the given activated property and skips every payment its apartment already has with the same date, amount and reference; all tenants are checked first, so one unknown tenant stores nothing.
     */
    public SaveRentPaymentsResponse save(String ownerId, String propertyId, SaveRentPaymentsRequest request) {
        Property property = loadPropertyOrThrow(ownerId, propertyId);
        if (property.rentTrackingStart() == null) {
            throw new RentTrackingNotActiveException(propertyId);
        }
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
            Tenant tenant = tenantsById.get(payment.tenantId());
            String reference = payment.reference().strip();
            if (rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                    tenant.apartment().id(), payment.paymentDate(), payment.amount(), reference)) {
                continue;
            }
            rentPaymentRepository.save(new RentPayment(
                    UUID.randomUUID().toString(),
                    tenant.apartment(),
                    tenant,
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
            Apartment apartment, RentSchedule schedule, Map<YearMonth, BigDecimal> paidByMonth, YearView view) {
        List<RentMonthResponse> months = IntStream.rangeClosed(1, MONTHS_PER_YEAR)
                .mapToObj(month -> toMonth(YearMonth.of(view.year(), month), schedule, paidByMonth, view))
                .toList();
        RentAlertLevel alert = view.trackingStart() == null
                ? RentAlertLevel.NONE
                : statusCalculator.alertLevel(schedule, paidByMonth, view.trackingStart(), view.today());
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

    private RentMonthResponse toMonth(YearMonth month, RentSchedule schedule, Map<YearMonth, BigDecimal> paidByMonth, YearView view) {
        BigDecimal expected = schedule.rentFor(month);
        BigDecimal paid = paidByMonth.getOrDefault(month, BigDecimal.ZERO);
        RentMonthStatus status = view.trackingStart() == null
                ? RentMonthStatus.UNTRACKED
                : statusCalculator.monthStatus(month, expected, paid, view.trackingStart(), view.today());
        return new RentMonthResponse(month.getMonthValue(), status, paid, expected);
    }

    private static FormerTenantPaymentResponse toFormerTenantPayment(RentPayment payment) {
        return new FormerTenantPaymentResponse(
                payment.apartment().id(),
                payment.apartment().floor(),
                payment.formerTenantName(),
                payment.paymentDate(),
                payment.amount(),
                payment.reference(),
                payment.periodYear(),
                payment.periodMonth());
    }

    private static Map<String, List<ApartmentRentChange>> changesByApartment(List<ApartmentRentChange> changes) {
        return changes.stream().collect(Collectors.groupingBy(change -> change.apartment().id()));
    }

    private static Map<String, Map<YearMonth, BigDecimal>> paidAmounts(List<ApartmentMonthPayment> sums) {
        return sums.stream().collect(Collectors.groupingBy(
                ApartmentMonthPayment::getApartmentId,
                Collectors.toMap(sum -> YearMonth.of(sum.getPeriodYear(), sum.getPeriodMonth()), ApartmentMonthPayment::getTotalAmount)));
    }

    private Property loadPropertyOrThrow(String ownerId, String propertyId) {
        return propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }

    private record YearView(int year, YearMonth trackingStart, LocalDate today) {
    }
}
