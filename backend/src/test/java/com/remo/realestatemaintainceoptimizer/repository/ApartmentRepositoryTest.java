package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Verifies that apartments and their tenants round-trip through the Flyway-managed PostgreSQL schema, are only found for the owning account and are removed together with their property.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ApartmentRepositoryTest {

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

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        property = propertyRepository.save(
                new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
    }

    @Test
    void persistsAndReloadsAnApartmentWithItsTenants() {
        Apartment apartment = newApartment("apartment-1");
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        apartment.addTenant("tenant-2", "Max", "Mustermann");
        apartmentRepository.save(apartment);
        flushAndClear();

        Apartment reloaded = apartmentRepository.findById("apartment-1").orElseThrow();

        assertThat(reloaded.property().id()).isEqualTo("property-1");
        assertThat(reloaded.floor()).isEqualTo(2);
        assertThat(reloaded.areaSquareMeters()).isEqualByComparingTo("64.50");
        assertThat(reloaded.totalRent()).isEqualByComparingTo("850.00");
        assertThat(reloaded.coldRent()).isEqualByComparingTo("650.00");
        assertThat(reloaded.additionalCosts()).isEqualByComparingTo("200.00");
        assertThat(reloaded.tenants()).extracting(Tenant::firstName).containsExactlyInAnyOrder("Erika", "Max");
    }

    @Test
    void persistsUpdatedApartmentDetailsAndTenantName() {
        Apartment apartment = newApartment("apartment-1");
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        apartmentRepository.save(apartment);
        flushAndClear();

        Tenant tenant = tenantRepository.findById("tenant-1").orElseThrow();
        tenant.updateName("Erika", "Musterfrau");
        tenant.apartment().updateDetails(-1, new BigDecimal("40.00"), new BigDecimal("500.00"),
                new BigDecimal("400.00"), new BigDecimal("100.00"));
        flushAndClear();

        Apartment reloaded = apartmentRepository.findById("apartment-1").orElseThrow();
        assertThat(reloaded.floor()).isEqualTo(-1);
        assertThat(reloaded.areaSquareMeters()).isEqualByComparingTo("40.00");
        assertThat(reloaded.tenants().get(0).lastName()).isEqualTo("Musterfrau");
    }

    @Test
    void listsEveryApartmentOfThePropertyWithItsTenantsOnly() {
        Property otherProperty = propertyRepository.save(
                new Property("property-2", owner.id(), "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        Apartment first = newApartment("apartment-1");
        first.addTenant("tenant-1", "Erika", "Mustermann");
        apartmentRepository.save(first);
        apartmentRepository.save(new Apartment(
                "apartment-2", otherProperty, 1, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO));
        flushAndClear();

        assertThat(apartmentRepository.findAllByPropertyId("property-1")).extracting(Apartment::id)
                .containsExactly("apartment-1");
        assertThat(apartmentRepository.countByPropertyId("property-1")).isEqualTo(1);
    }

    @Test
    void findsAnApartmentAndATenantOnlyForTheOwningAccount() {
        User otherOwner = TestAccounts.saveRegularAccount(userRepository);
        Apartment apartment = newApartment("apartment-1");
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        apartmentRepository.save(apartment);
        flushAndClear();

        assertThat(apartmentRepository.findByIdAndPropertyOwnerId("apartment-1", owner.id())).isPresent();
        assertThat(apartmentRepository.findByIdAndPropertyOwnerId("apartment-1", otherOwner.id())).isEmpty();
        assertThat(tenantRepository.findByIdAndApartmentPropertyOwnerId("tenant-1", owner.id())).isPresent();
        assertThat(tenantRepository.findByIdAndApartmentPropertyOwnerId("tenant-1", otherOwner.id())).isEmpty();
    }

    @Test
    void findsAnApartmentForUpdateOnlyForTheOwningAccount() {
        User otherOwner = TestAccounts.saveRegularAccount(userRepository);
        apartmentRepository.save(newApartment("apartment-1"));
        flushAndClear();

        assertThat(apartmentRepository.findByIdAndOwnerIdForUpdate("apartment-1", owner.id())).isPresent();
        assertThat(apartmentRepository.findByIdAndOwnerIdForUpdate("apartment-1", otherOwner.id())).isEmpty();
        assertThat(apartmentRepository.findByIdAndOwnerIdForUpdate("unknown", owner.id())).isEmpty();
    }

    @Test
    void removingATenantFromItsApartmentDeletesIt() {
        Apartment apartment = newApartment("apartment-1");
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        Tenant second = apartment.addTenant("tenant-2", "Max", "Mustermann");
        apartmentRepository.save(apartment);
        flushAndClear();

        Apartment loaded = apartmentRepository.findById("apartment-1").orElseThrow();
        loaded.removeTenant(loaded.tenants().stream().filter(tenant -> tenant.id().equals(second.id())).findFirst().orElseThrow());
        flushAndClear();

        assertThat(tenantRepository.findById("tenant-2")).isEmpty();
        assertThat(tenantRepository.findById("tenant-1")).isPresent();
    }

    @Test
    void deletingThePropertyCascadesToItsApartmentsAndTenants() {
        Apartment apartment = newApartment("apartment-1");
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        apartmentRepository.save(apartment);
        flushAndClear();

        propertyRepository.delete(propertyRepository.findById("property-1").orElseThrow());
        flushAndClear();

        assertThat(apartmentRepository.findById("apartment-1")).isEmpty();
        assertThat(tenantRepository.findById("tenant-1")).isEmpty();
    }

    private Apartment newApartment(String id) {
        return new Apartment(
                id,
                property,
                2,
                new BigDecimal("64.50"),
                new BigDecimal("850.00"),
                new BigDecimal("650.00"),
                new BigDecimal("200.00"));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
