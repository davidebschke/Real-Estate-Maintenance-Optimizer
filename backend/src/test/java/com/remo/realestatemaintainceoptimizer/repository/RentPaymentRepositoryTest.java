package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies that rent payments and rent changes round-trip through the Flyway-managed PostgreSQL schema, are summed per apartment and month for a property or an account, are found as duplicates per apartment, stay with their apartment when their tenant is deleted and are removed together with it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RentPaymentRepositoryTest {

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @Autowired
    private ApartmentRentChangeRepository rentChangeRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    private Property property;

    private Apartment apartment;

    @BeforeEach
    void seedApartment() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        property = propertyRepository.save(
                new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        apartment = RentFixtures.saveApartment(apartmentRepository, property, "apartment-1", "850.00",
                new TenantSpec("tenant-1", "Erika", "Mustermann"), new TenantSpec("tenant-2", "Max", "Mustermann"));
    }

    private RentPayment payment(String id, String tenantId, String amount, int year, int month) {
        Tenant tenant = apartment.tenants().stream().filter(candidate -> candidate.id().equals(tenantId)).findFirst().orElseThrow();
        return new RentPayment(id, apartment, tenant, LocalDate.of(year, month, 2), new BigDecimal(amount), "Miete", year, month);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void persistsAndReloadsAPayment() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();

        RentPayment reloaded = rentPaymentRepository.findById("payment-1").orElseThrow();

        assertThat(reloaded.apartment().id()).isEqualTo("apartment-1");
        assertThat(reloaded.tenant().id()).isEqualTo("tenant-1");
        assertThat(reloaded.formerTenantName()).isNull();
        assertThat(reloaded.paymentDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(reloaded.amount()).isEqualByComparingTo("850.00");
        assertThat(reloaded.reference()).isEqualTo("Miete");
        assertThat(reloaded.periodYear()).isEqualTo(2026);
        assertThat(reloaded.periodMonth()).isEqualTo(10);
    }

    @Test
    void sumsThePaymentsOfAllTenantsOfAnApartmentPerMonthForAProperty() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "500.00", 2026, 10));
        rentPaymentRepository.save(payment("payment-2", "tenant-2", "350.00", 2026, 10));
        rentPaymentRepository.save(payment("payment-3", "tenant-1", "850.00", 2026, 9));
        flushAndClear();

        List<ApartmentMonthPayment> sums = rentPaymentRepository.sumByApartmentAndMonthForProperty("property-1");

        assertThat(sums)
                .extracting(sum -> sum.getApartmentId(), sum -> sum.getPeriodYear(), sum -> sum.getPeriodMonth(),
                        sum -> sum.getTotalAmount().setScale(2))
                .containsExactlyInAnyOrder(
                        tuple("apartment-1", 2026, 10, new BigDecimal("850.00")),
                        tuple("apartment-1", 2026, 9, new BigDecimal("850.00")));
    }

    @Test
    void sumsOnlyTheApartmentsOfTheRequestedPropertyOrAccount() {
        Property otherProperty = propertyRepository.save(
                new Property("property-2", owner.id(), "Mediapark", "Im Mediapark 1", "pi-building"));
        Apartment otherApartment = RentFixtures.saveApartment(apartmentRepository, otherProperty, "apartment-2", "700.00",
                new TenantSpec("tenant-3", "Anna", "Schmidt"));
        User stranger = TestAccounts.saveRegularAccount(userRepository);
        Property strangerProperty = propertyRepository.save(
                new Property("property-3", stranger.id(), "Fremdhaus", "Hauptstr. 1", "pi-building"));
        Apartment strangerApartment = RentFixtures.saveApartment(apartmentRepository, strangerProperty, "apartment-3", "600.00",
                new TenantSpec("tenant-4", "Hans", "Meier"));
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        rentPaymentRepository.save(new RentPayment("payment-2", otherApartment, otherApartment.tenants().getFirst(),
                LocalDate.of(2026, 10, 3), new BigDecimal("700.00"), "Miete", 2026, 10));
        rentPaymentRepository.save(new RentPayment("payment-3", strangerApartment, strangerApartment.tenants().getFirst(),
                LocalDate.of(2026, 10, 3), new BigDecimal("600.00"), "Miete", 2026, 10));
        flushAndClear();

        assertThat(rentPaymentRepository.sumByApartmentAndMonthForProperty("property-1"))
                .extracting(ApartmentMonthPayment::getApartmentId).containsExactly("apartment-1");
        assertThat(rentPaymentRepository.sumByApartmentAndMonthForOwner(owner.id()))
                .extracting(ApartmentMonthPayment::getApartmentId).containsExactlyInAnyOrder("apartment-1", "apartment-2");
    }

    @Test
    void findsADuplicateOnlyWithTheSameApartmentDateAmountAndReference() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();
        LocalDate date = LocalDate.of(2026, 10, 2);

        assertThat(rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                "apartment-1", date, new BigDecimal("850.00"), "Miete")).isTrue();
        assertThat(rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                "apartment-2", date, new BigDecimal("850.00"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                "apartment-1", date.plusDays(1), new BigDecimal("850.00"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                "apartment-1", date, new BigDecimal("850.01"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByApartmentIdAndPaymentDateAndAmountAndReference(
                "apartment-1", date, new BigDecimal("850.00"), "Andere")).isFalse();
    }

    @Test
    void refusesAnIdenticalSecondPaymentOfTheSameApartmentEvenFromAnotherTenant() {
        rentPaymentRepository.saveAndFlush(payment("payment-1", "tenant-1", "850.00", 2026, 10));

        assertThatThrownBy(() -> rentPaymentRepository.saveAndFlush(payment("payment-2", "tenant-2", "850.00", 2026, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void markingATenantAsFormerKeepsTheirPaymentsWithTheApartmentAndNotesTheirName() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        rentPaymentRepository.save(payment("payment-2", "tenant-2", "850.00", 2026, 9));
        flushAndClear();

        int marked = rentPaymentRepository.markTenantAsFormer("tenant-1", "Mustermann, Erika");
        flushAndClear();

        assertThat(marked).isEqualTo(1);
        RentPayment reloaded = rentPaymentRepository.findById("payment-1").orElseThrow();
        assertThat(reloaded.tenant()).isNull();
        assertThat(reloaded.formerTenantName()).isEqualTo("Mustermann, Erika");
        assertThat(reloaded.apartment().id()).isEqualTo("apartment-1");
        assertThat(rentPaymentRepository.findById("payment-2").orElseThrow().tenant()).isNotNull();
    }

    @Test
    void listsOnlyThePaymentsOfFormerTenantsForTheRequestedYearOldestFirst() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        rentPaymentRepository.save(payment("payment-2", "tenant-1", "850.00", 2026, 9));
        rentPaymentRepository.save(payment("payment-3", "tenant-1", "850.00", 2025, 12));
        rentPaymentRepository.save(payment("payment-4", "tenant-2", "850.00", 2026, 8));
        flushAndClear();
        rentPaymentRepository.markTenantAsFormer("tenant-1", "Mustermann, Erika");
        flushAndClear();

        List<RentPayment> former = rentPaymentRepository.findFormerTenantPayments("property-1", 2026);

        assertThat(former).extracting(RentPayment::id).containsExactly("payment-2", "payment-1");
        assertThat(former).allSatisfy(payment -> assertThat(payment.formerTenantName()).isEqualTo("Mustermann, Erika"));
    }

    @Test
    void deletingATenantKeepsTheirPaymentsWithoutATenant() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();

        tenantRepository.deleteById("tenant-1");
        flushAndClear();

        RentPayment reloaded = rentPaymentRepository.findById("payment-1").orElseThrow();
        assertThat(reloaded.tenant()).isNull();
        assertThat(reloaded.apartment().id()).isEqualTo("apartment-1");
    }

    @Test
    void deletingTheApartmentRemovesItsPaymentsAndRentChanges() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        rentChangeRepository.save(new ApartmentRentChange("change-1", apartment, YearMonth.of(2026, 10), new BigDecimal("800.00")));
        flushAndClear();

        apartmentRepository.deleteById("apartment-1");
        flushAndClear();

        assertThat(rentPaymentRepository.count()).isZero();
        assertThat(rentChangeRepository.count()).isZero();
    }

    @Test
    void deletingTheOwnerRemovesTheirPayments() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();

        userRepository.deleteAllInBatch();
        flushAndClear();

        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void persistsTheRentTrackingStartOfAProperty() {
        Property stored = propertyRepository.findById("property-1").orElseThrow();
        assertThat(stored.rentTrackingStart()).isNull();

        stored.activateRentTracking(YearMonth.of(2026, 1));
        stored.activateRentTracking(YearMonth.of(2026, 6));
        flushAndClear();

        assertThat(propertyRepository.findById("property-1").orElseThrow().rentTrackingStart()).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    void remembersARentChangeOnlyOncePerApartmentAndMonth() {
        rentChangeRepository.saveAndFlush(new ApartmentRentChange("change-1", apartment, YearMonth.of(2026, 10), new BigDecimal("800.00")));

        assertThat(rentChangeRepository.existsByApartmentIdAndEffectiveMonth("apartment-1", LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(rentChangeRepository.existsByApartmentIdAndEffectiveMonth("apartment-1", LocalDate.of(2026, 11, 1))).isFalse();
        assertThatThrownBy(() -> rentChangeRepository.saveAndFlush(
                new ApartmentRentChange("change-2", apartment, YearMonth.of(2026, 10), new BigDecimal("700.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
