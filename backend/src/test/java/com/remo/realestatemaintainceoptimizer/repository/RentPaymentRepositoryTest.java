package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.RentFixtures.TenantSpec;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies that rent payments round-trip through the Flyway-managed PostgreSQL schema, are summed per apartment and month for a property or an account, are found as duplicates and are removed together with their tenant.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RentPaymentRepositoryTest {

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

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
        return new RentPayment(id, tenant, LocalDate.of(year, month, 2), new BigDecimal(amount), "Miete", year, month);
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

        assertThat(reloaded.tenant().id()).isEqualTo("tenant-1");
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
        rentPaymentRepository.save(new RentPayment("payment-2", otherApartment.tenants().getFirst(), LocalDate.of(2026, 10, 3),
                new BigDecimal("700.00"), "Miete", 2026, 10));
        rentPaymentRepository.save(new RentPayment("payment-3", strangerApartment.tenants().getFirst(), LocalDate.of(2026, 10, 3),
                new BigDecimal("600.00"), "Miete", 2026, 10));
        flushAndClear();

        assertThat(rentPaymentRepository.sumByApartmentAndMonthForProperty("property-1"))
                .extracting(ApartmentMonthPayment::getApartmentId).containsExactly("apartment-1");
        assertThat(rentPaymentRepository.sumByApartmentAndMonthForOwner(owner.id()))
                .extracting(ApartmentMonthPayment::getApartmentId).containsExactlyInAnyOrder("apartment-1", "apartment-2");
    }

    @Test
    void findsADuplicateOnlyWithTheSameTenantDateAmountAndReference() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();
        LocalDate date = LocalDate.of(2026, 10, 2);

        assertThat(rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                "tenant-1", date, new BigDecimal("850.00"), "Miete")).isTrue();
        assertThat(rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                "tenant-2", date, new BigDecimal("850.00"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                "tenant-1", date.plusDays(1), new BigDecimal("850.00"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                "tenant-1", date, new BigDecimal("850.01"), "Miete")).isFalse();
        assertThat(rentPaymentRepository.existsByTenantIdAndPaymentDateAndAmountAndReference(
                "tenant-1", date, new BigDecimal("850.00"), "Andere")).isFalse();
    }

    @Test
    void refusesAnIdenticalSecondPaymentOfTheSameTenant() {
        rentPaymentRepository.saveAndFlush(payment("payment-1", "tenant-1", "850.00", 2026, 10));

        assertThatThrownBy(() -> rentPaymentRepository.saveAndFlush(payment("payment-2", "tenant-1", "850.00", 2026, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTheSamePaymentForADifferentTenant() {
        rentPaymentRepository.saveAndFlush(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        rentPaymentRepository.saveAndFlush(payment("payment-2", "tenant-2", "850.00", 2026, 10));

        assertThat(rentPaymentRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingTheApartmentRemovesItsPayments() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();

        apartmentRepository.deleteById("apartment-1");
        flushAndClear();

        assertThat(rentPaymentRepository.count()).isZero();
    }

    @Test
    void deletingTheOwnerRemovesTheirPayments() {
        rentPaymentRepository.save(payment("payment-1", "tenant-1", "850.00", 2026, 10));
        flushAndClear();

        userRepository.deleteAllInBatch();
        flushAndClear();

        assertThat(rentPaymentRepository.count()).isZero();
    }
}
