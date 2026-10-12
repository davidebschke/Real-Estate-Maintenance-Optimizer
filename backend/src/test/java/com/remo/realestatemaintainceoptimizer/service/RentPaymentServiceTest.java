package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentUpdateRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentYearResponse;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsRequest;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsResponse;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantRentYearResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RentTrackingNotActiveException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRentChangeRepository;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies activating the rent payment check, storing confirmed payments (duplicates per apartment skipped, all-or-nothing tenant check), the yearly payment table with its month states, the rent due per month and the payments of former tenants, and the alerts, plus account isolation, against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RentPaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final TenantSpec ERIKA = new TenantSpec("tenant-1", "Erika", "Mustermann");
    private static final TenantSpec MAX = new TenantSpec("tenant-2", "Max", "Mustermann");
    private static final TenantSpec ANNA = new TenantSpec("tenant-3", "Anna", "Adler");

    @Autowired
    private RentPaymentService service;

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @Autowired
    private ApartmentRentChangeRepository rentChangeRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApartmentService apartmentService;

    private User owner;

    private User otherOwner;

    private LocalDate today;

    private YearMonth currentMonth;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        otherOwner = TestAccounts.saveRegularAccount(userRepository);
        today = LocalDate.now(ZONE);
        currentMonth = YearMonth.from(today);
        Property property = saveProperty("property-1", currentMonth);
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-1", "850.00", ERIKA, MAX);
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-2", "700.00", ANNA);
    }

    private Property saveProperty(String id, YearMonth trackingStart) {
        Property property = new Property(id, owner.id(), "Wohnanlage " + id, "Aachener Str. 512", "pi-building");
        if (trackingStart != null) {
            property.activateRentTracking(trackingStart);
        }
        return propertyRepository.save(property);
    }

    private static RentPaymentRequest request(String tenantId, String amount, LocalDate paymentDate, YearMonth period) {
        return new RentPaymentRequest(tenantId, paymentDate, new BigDecimal(amount), "Miete", period.getYear(), period.getMonthValue());
    }

    private SaveRentPaymentsResponse save(RentPaymentRequest... payments) {
        return service.save(owner.id(), "property-1", new SaveRentPaymentsRequest(List.of(payments)));
    }

    private TenantRentYearResponse row(RentYearResponse overview, String tenantId) {
        return overview.tenants().stream().filter(tenant -> tenant.tenantId().equals(tenantId)).findFirst().orElseThrow();
    }

    private boolean isPastDueDay() {
        return today.getDayOfMonth() > 5;
    }

    @Test
    void activatingTheCheckCountsFromTheCurrentMonthAndKeepsAnEarlierStart() {
        saveProperty("property-2", null);

        String first = service.activateTracking(owner.id(), "property-2").trackingStart();
        String second = service.activateTracking(owner.id(), "property-2").trackingStart();

        assertThat(first).isEqualTo(currentMonth.toString());
        assertThat(second).isEqualTo(first);
        assertThat(service.activateTracking(owner.id(), "property-1").trackingStart()).isEqualTo(currentMonth.toString());
    }

    @Test
    void anotherAccountCannotActivateAPropertyOrSeeItsPayments() {
        SaveRentPaymentsRequest request = new SaveRentPaymentsRequest(List.of(request("tenant-1", "850.00", today, currentMonth)));

        assertThatThrownBy(() -> service.activateTracking(otherOwner.id(), "property-1")).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.save(otherOwner.id(), "property-1", request)).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.yearOverview(otherOwner.id(), "property-1", null))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThat(service.alerts(otherOwner.id())).isEmpty();
    }

    @Test
    void aPropertyWithoutActivationTracksNothingStoresNothingAndRaisesNoAlert() {
        saveProperty("property-2", null);
        RentFixtures.saveApartment(apartmentRepository, propertyRepository.findById("property-2").orElseThrow(), "apartment-9", "600.00",
                new TenantSpec("tenant-9", "Hans", "Meier"));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-2", null);

        assertThat(overview.trackingStart()).isNull();
        assertThat(overview.tenants()).hasSize(1);
        assertThat(overview.tenants().getFirst().alert()).isEqualTo(RentAlertLevel.NONE);
        assertThat(overview.tenants().getFirst().months()).allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UNTRACKED));
        assertThatThrownBy(() -> service.save(owner.id(), "property-2",
                new SaveRentPaymentsRequest(List.of(request("tenant-9", "600.00", today, currentMonth)))))
                .isInstanceOf(RentTrackingNotActiveException.class);
        assertThat(service.alerts(owner.id())).extracting(RentAlertResponse::propertyId).containsOnly("property-1");
    }

    @Test
    void storesTheConfirmedPaymentsAndSkipsAnIdenticalOneOnTheSecondSave() {
        RentPaymentRequest payment = request("tenant-1", "850.00", today, currentMonth);

        SaveRentPaymentsResponse first = save(payment);
        SaveRentPaymentsResponse second = save(payment, request("tenant-3", "700.00", today, currentMonth));

        assertThat(first).isEqualTo(new SaveRentPaymentsResponse(1, 0));
        assertThat(second).isEqualTo(new SaveRentPaymentsResponse(1, 1));
        assertThat(rentPaymentRepository.count()).isEqualTo(2);
    }

    @Test
    void skipsAnIdenticalPaymentRepeatedWithinOneRequestAlsoForACoTenant() {
        RentPaymentRequest payment = request("tenant-1", "850.00", today, currentMonth);
        RentPaymentRequest sameForCoTenant = request("tenant-2", "850.00", today, currentMonth);

        assertThat(save(payment, payment, sameForCoTenant)).isEqualTo(new SaveRentPaymentsResponse(1, 2));
    }

    @Test
    void anUnknownTenantStoresNothingAtAll() {
        assertThatThrownBy(() -> save(
                request("tenant-1", "850.00", today, currentMonth), request("unknown", "850.00", today, currentMonth)))
                .isInstanceOf(TenantNotFoundException.class);

        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void aTenantOfAnotherPropertyIsRejected() {
        Property otherProperty = saveProperty("property-2", currentMonth);
        RentFixtures.saveApartment(apartmentRepository, otherProperty, "apartment-3", "600.00",
                new TenantSpec("tenant-9", "Hans", "Meier"));

        assertThatThrownBy(() -> save(request("tenant-9", "600.00", today, currentMonth)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void theYearOverviewListsEveryTenantSortedByFloorAndNameWithTwelveMonths() {
        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", null);

        assertThat(overview.year()).isEqualTo(today.getYear());
        assertThat(overview.trackingStart()).isEqualTo(currentMonth.toString());
        assertThat(overview.tenants()).extracting(TenantRentYearResponse::lastName, TenantRentYearResponse::firstName)
                .containsExactly(tuple("Adler", "Anna"), tuple("Mustermann", "Erika"), tuple("Mustermann", "Max"));
        assertThat(overview.tenants()).allSatisfy(tenant -> assertThat(tenant.months()).hasSize(12));
        assertThat(row(overview, "tenant-1").monthlyRent()).isEqualByComparingTo("850.00");
        assertThat(row(overview, "tenant-3").monthlyRent()).isEqualByComparingTo("700.00");
        assertThat(overview.formerTenantPayments()).isEmpty();
    }

    @Test
    void thePaymentsOfAllCoTenantsCountTowardsTheSharedRentOfTheApartment() {
        save(request("tenant-1", "500.00", today, currentMonth), request("tenant-2", "350.00", today, currentMonth));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        for (String tenantId : List.of("tenant-1", "tenant-2")) {
            assertThat(row(overview, tenantId).months().get(currentMonth.getMonthValue() - 1)).satisfies(month -> {
                assertThat(month.status()).isEqualTo(RentMonthStatus.PAID);
                assertThat(month.paidAmount()).isEqualByComparingTo("850.00");
                assertThat(month.expectedAmount()).isEqualByComparingTo("850.00");
            });
            assertThat(row(overview, tenantId).alert()).isEqualTo(RentAlertLevel.NONE);
        }
    }

    @Test
    void monthsBeforeTheActivationAreUntrackedEvenWhenItHappenedLateInTheCurrentMonthAndLaterMonthsUpcoming() {
        RentYearResponse thisYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());
        RentYearResponse lastYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear() - 1);
        RentYearResponse nextYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear() + 1);

        assertThat(row(thisYear, "tenant-1").months().subList(0, currentMonth.getMonthValue() - 1))
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UNTRACKED));
        assertThat(row(thisYear, "tenant-1").months().get(currentMonth.getMonthValue() - 1).status())
                .isIn(RentMonthStatus.PENDING, RentMonthStatus.OVERDUE);
        assertThat(row(thisYear, "tenant-1").months().subList(currentMonth.getMonthValue(), 12))
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UPCOMING));
        assertThat(row(lastYear, "tenant-1").months())
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UNTRACKED));
        assertThat(row(nextYear, "tenant-1").months())
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UPCOMING));
    }

    @Test
    void anUnpaidCurrentMonthIsPendingBeforeAndOverdueAfterTheDueDayAndFlagsTheTenant() {
        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        RentMonthStatus expected = isPastDueDay() ? RentMonthStatus.OVERDUE : RentMonthStatus.PENDING;
        RentAlertLevel expectedAlert = isPastDueDay() ? RentAlertLevel.OVERDUE : RentAlertLevel.PENDING;
        assertThat(row(overview, "tenant-3").months().get(currentMonth.getMonthValue() - 1).status()).isEqualTo(expected);
        assertThat(row(overview, "tenant-3").alert()).isEqualTo(expectedAlert);
    }

    @Test
    void aPartlyPaidCurrentMonthIsPartialAfterTheDueDay() {
        save(request("tenant-1", "300.00", today, currentMonth));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        RentMonthStatus expected = isPastDueDay() ? RentMonthStatus.PARTIAL : RentMonthStatus.PENDING;
        assertThat(row(overview, "tenant-2").months().get(currentMonth.getMonthValue() - 1).status()).isEqualTo(expected);
    }

    @Test
    void aMissedMonthAfterTheActivationKeepsTheTenantOverdue() {
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);
        Property earlier = saveProperty("property-2", twoMonthsAgo);
        RentFixtures.saveApartment(apartmentRepository, earlier, "apartment-4", "700.00", new TenantSpec("tenant-4", "Eva", "Berg"));
        service.save(owner.id(), "property-2", new SaveRentPaymentsRequest(List.of(
                request("tenant-4", "700.00", twoMonthsAgo.atDay(2), twoMonthsAgo), request("tenant-4", "700.00", today, currentMonth))));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-2", currentMonth.getYear());

        assertThat(row(overview, "tenant-4").alert()).isEqualTo(RentAlertLevel.OVERDUE);
    }

    @Test
    void aRentChangeOnlyAppliesFromItsMonthOnAndEarlierMonthsKeepTheirRent() {
        Apartment apartment = apartmentRepository.findAllByPropertyId("property-1").stream()
                .filter(candidate -> "apartment-2".equals(candidate.id())).findFirst().orElseThrow();
        apartment.rememberInitialRent(new BigDecimal("600.00"));
        apartmentRepository.save(apartment);
        rentChangeRepository.save(new ApartmentRentChange(UUID.randomUUID().toString(), apartment, currentMonth, new BigDecimal("700.00")));
        YearMonth previousMonth = currentMonth.minusMonths(1);

        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());
        RentYearResponse previousYear = service.yearOverview(owner.id(), "property-1", previousMonth.getYear());

        assertThat(row(overview, "tenant-3").months().get(currentMonth.getMonthValue() - 1).expectedAmount()).isEqualByComparingTo("700.00");
        assertThat(row(previousYear, "tenant-3").months().get(previousMonth.getMonthValue() - 1).expectedAmount())
                .isEqualByComparingTo("600.00");
    }

    @Test
    void theRealRentEditThroughTheApartmentServiceKeepsAnEarlierMonthsPaymentSufficient() {
        YearMonth previousMonth = currentMonth.minusMonths(1);
        Property earlier = saveProperty("property-2", previousMonth);
        Apartment apartment = RentFixtures.saveApartment(apartmentRepository, earlier, "apartment-5", "700.00",
                new TenantSpec("tenant-5", "Uwe", "Roth"));
        service.save(owner.id(), "property-2", new SaveRentPaymentsRequest(List.of(
                request("tenant-5", "700.00", previousMonth.atDay(2), previousMonth), request("tenant-5", "750.00", today, currentMonth))));

        apartmentService.updateApartment(owner.id(), "apartment-5", new ApartmentUpdateRequest(
                new ApartmentDetailsRequest(
                        apartment.floor(), apartment.areaSquareMeters(), new BigDecimal("750.00"), new BigDecimal("550.00"), new BigDecimal("200.00")),
                null));

        assertThat(service.alerts(owner.id()).stream().filter(alert -> "tenant-5".equals(alert.tenantId()))).isEmpty();
    }

    @Test
    void thePaymentsOfADeletedTenantAreNotedAsFormerTenantPaymentsAndStillCountForTheApartment() {
        save(request("tenant-1", "500.00", today, currentMonth), request("tenant-2", "350.00", today, currentMonth));

        apartmentService.deleteTenant(owner.id(), "tenant-1");
        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        assertThat(overview.formerTenantPayments()).singleElement().satisfies(payment -> {
            assertThat(payment.formerTenantName()).isEqualTo("Mustermann, Erika");
            assertThat(payment.apartmentId()).isEqualTo("apartment-1");
            assertThat(payment.amount()).isEqualByComparingTo("500.00");
            assertThat(payment.periodMonth()).isEqualTo(currentMonth.getMonthValue());
        });
        assertThat(row(overview, "tenant-2").months().get(currentMonth.getMonthValue() - 1).status()).isEqualTo(RentMonthStatus.PAID);
        assertThat(service.yearOverview(owner.id(), "property-1", currentMonth.getYear() - 1).formerTenantPayments()).isEmpty();
    }

    @Test
    void anApartmentCreatedAfterTheActivationIsOnlyJudgedFromItsCreationMonthOn() {
        saveProperty("property-2", currentMonth.minusMonths(3));
        ApartmentResponse created = apartmentService.create(owner.id(), "property-2", new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(1, new BigDecimal("60.00"), new BigDecimal("700.00"), new BigDecimal("500.00"), new BigDecimal("200.00")),
                new TenantDetailsRequest("Eva", "Berg")));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-2", currentMonth.getYear());
        RentYearResponse previousYear = service.yearOverview(owner.id(), "property-2", currentMonth.minusMonths(3).getYear());

        String tenantId = created.tenants().getFirst().id();
        assertThat(row(overview, tenantId).months().subList(0, currentMonth.getMonthValue() - 1))
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UNTRACKED));
        assertThat(row(previousYear, tenantId).months())
                .allSatisfy(month -> assertThat(month.status()).isIn(RentMonthStatus.UNTRACKED, RentMonthStatus.PENDING, RentMonthStatus.OVERDUE, RentMonthStatus.UPCOMING));
        assertThat(service.alerts(owner.id()).stream().filter(alert -> tenantId.equals(alert.tenantId())).findFirst().orElseThrow().alert())
                .isEqualTo(isPastDueDay() ? RentAlertLevel.OVERDUE : RentAlertLevel.PENDING);
        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void rejectsAYearOutsideTheSupportedRange() {
        assertThatThrownBy(() -> service.yearOverview(owner.id(), "property-1", 1999))
                .isInstanceOfSatisfying(InvalidRentRequestException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidRentRequestException.REASON_INVALID_YEAR));
        assertThatThrownBy(() -> service.yearOverview(owner.id(), "property-1", 2101))
                .isInstanceOf(InvalidRentRequestException.class);
    }

    @Test
    void alertsListEveryTenantOfAnApartmentWhoseRentIsNotPaid() {
        save(request("tenant-3", "700.00", today, currentMonth));

        List<RentAlertResponse> alerts = service.alerts(owner.id());

        RentAlertLevel expected = isPastDueDay() ? RentAlertLevel.OVERDUE : RentAlertLevel.PENDING;
        assertThat(alerts).extracting(RentAlertResponse::tenantId).containsExactlyInAnyOrder("tenant-1", "tenant-2");
        assertThat(alerts).allSatisfy(alert -> {
            assertThat(alert.propertyId()).isEqualTo("property-1");
            assertThat(alert.apartmentId()).isEqualTo("apartment-1");
            assertThat(alert.alert()).isEqualTo(expected);
        });
    }

    @Test
    void noAlertsRemainOnceEveryApartmentPaidItsRent() {
        save(request("tenant-1", "850.00", today, currentMonth), request("tenant-3", "700.00", today, currentMonth));

        assertThat(service.alerts(owner.id())).isEmpty();
    }
}
