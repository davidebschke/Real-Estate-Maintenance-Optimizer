package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentYearResponse;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsRequest;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsResponse;
import com.remo.realestatemaintainceoptimizer.dto.TenantRentYearResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies storing confirmed payments (duplicates skipped, all-or-nothing tenant check), the yearly payment table with its month states and shared apartment rent, and the alerts, plus account isolation, against a real PostgreSQL database.
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
    private ApartmentRepository apartmentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    private User otherOwner;

    private Property property;

    private LocalDate today;

    private YearMonth currentMonth;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        otherOwner = TestAccounts.saveRegularAccount(userRepository);
        property = propertyRepository.save(
                new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-1", "850.00", ERIKA, MAX);
        RentFixtures.saveApartment(apartmentRepository, property, "apartment-2", "700.00", ANNA);
        today = LocalDate.now(ZONE);
        currentMonth = YearMonth.from(today);
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
    void storesTheConfirmedPaymentsAndSkipsAnIdenticalOneOnTheSecondSave() {
        RentPaymentRequest payment = request("tenant-1", "850.00", today, currentMonth);

        SaveRentPaymentsResponse first = save(payment);
        SaveRentPaymentsResponse second = save(payment, request("tenant-3", "700.00", today, currentMonth));

        assertThat(first).isEqualTo(new SaveRentPaymentsResponse(1, 0));
        assertThat(second).isEqualTo(new SaveRentPaymentsResponse(1, 1));
        assertThat(rentPaymentRepository.count()).isEqualTo(2);
    }

    @Test
    void skipsAnIdenticalPaymentRepeatedWithinOneRequest() {
        RentPaymentRequest payment = request("tenant-1", "850.00", today, currentMonth);

        assertThat(save(payment, payment)).isEqualTo(new SaveRentPaymentsResponse(1, 1));
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
        Property otherProperty = propertyRepository.save(
                new Property("property-2", owner.id(), "Mediapark", "Im Mediapark 1", "pi-building"));
        RentFixtures.saveApartment(apartmentRepository, otherProperty, "apartment-3", "600.00",
                new TenantSpec("tenant-9", "Hans", "Meier"));

        assertThatThrownBy(() -> save(request("tenant-9", "600.00", today, currentMonth)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void anotherAccountsPropertyBehavesAsIfItDidNotExist() {
        SaveRentPaymentsRequest request = new SaveRentPaymentsRequest(List.of(request("tenant-1", "850.00", today, currentMonth)));

        assertThatThrownBy(() -> service.save(otherOwner.id(), "property-1", request)).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.yearOverview(otherOwner.id(), "property-1", null))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThat(service.alerts(otherOwner.id())).isEmpty();
    }

    @Test
    void theYearOverviewListsEveryTenantSortedByFloorAndNameWithTwelveMonths() {
        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", null);

        assertThat(overview.year()).isEqualTo(today.getYear());
        assertThat(overview.tenants()).extracting(TenantRentYearResponse::lastName, TenantRentYearResponse::firstName)
                .containsExactly(tuple("Adler", "Anna"),
                        tuple("Mustermann", "Erika"),
                        tuple("Mustermann", "Max"));
        assertThat(overview.tenants()).allSatisfy(tenant -> assertThat(tenant.months()).hasSize(12));
        assertThat(row(overview, "tenant-1").monthlyRent()).isEqualByComparingTo("850.00");
        assertThat(row(overview, "tenant-3").monthlyRent()).isEqualByComparingTo("700.00");
    }

    @Test
    void thePaymentsOfAllCoTenantsCountTowardsTheSharedRentOfTheApartment() {
        save(request("tenant-1", "500.00", today, currentMonth), request("tenant-2", "350.00", today, currentMonth));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        for (String tenantId : List.of("tenant-1", "tenant-2")) {
            assertThat(row(overview, tenantId).months().get(currentMonth.getMonthValue() - 1)).satisfies(month -> {
                assertThat(month.status()).isEqualTo(RentMonthStatus.PAID);
                assertThat(month.paidAmount()).isEqualByComparingTo("850.00");
            });
            assertThat(row(overview, tenantId).alert()).isEqualTo(RentAlertLevel.NONE);
        }
    }

    @Test
    void monthsBeforeTheFirstPaymentAreUntrackedAndLaterMonthsUpcoming() {
        save(request("tenant-1", "850.00", today, currentMonth));

        RentYearResponse thisYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());
        RentYearResponse lastYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear() - 1);
        RentYearResponse nextYear = service.yearOverview(owner.id(), "property-1", currentMonth.getYear() + 1);

        assertThat(row(thisYear, "tenant-1").months().subList(0, currentMonth.getMonthValue() - 1))
                .allSatisfy(month -> assertThat(month.status()).isEqualTo(RentMonthStatus.UNTRACKED));
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
    void aMissedMonthAfterTheFirstPaymentKeepsTheTenantOverdue() {
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);
        save(request("tenant-3", "700.00", twoMonthsAgo.atDay(2), twoMonthsAgo), request("tenant-3", "700.00", today, currentMonth));

        RentYearResponse overview = service.yearOverview(owner.id(), "property-1", currentMonth.getYear());

        assertThat(row(overview, "tenant-3").alert()).isEqualTo(RentAlertLevel.OVERDUE);
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
