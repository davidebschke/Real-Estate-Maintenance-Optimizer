package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.TenantLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.TenantRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies apartment creation with a first tenant, adding and editing tenants, deleting tenants (and the apartment with its last tenant), ordering, account isolation and size limits against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApartmentServiceTest {

    @Autowired
    private ApartmentService service;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    private User otherOwner;

    private Property property;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        otherOwner = TestAccounts.saveRegularAccount(userRepository);
        property = propertyRepository.save(
                new Property("property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
    }

    @Test
    void createsAnApartmentWithItsFirstTenant() {
        ApartmentResponse response = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        assertThat(response.id()).isNotBlank();
        assertThat(response.propertyId()).isEqualTo("property-1");
        assertThat(response.floor()).isEqualTo(2);
        assertThat(response.areaSquareMeters()).isEqualByComparingTo("64.50");
        assertThat(response.totalRent()).isEqualByComparingTo("850.00");
        assertThat(response.coldRent()).isEqualByComparingTo("650.00");
        assertThat(response.additionalCosts()).isEqualByComparingTo("200.00");
        assertThat(response.tenants()).extracting("firstName", "lastName").containsExactly(tuple("Erika", "Mustermann"));
        assertThat(service.listByProperty(owner.id(), "property-1")).hasSize(1);
    }

    @Test
    void addsFurtherTenantsToTheSameApartmentSortedByName() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Max", "Zimmermann"));

        ApartmentResponse updated = service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Anna", "Albrecht"));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.tenants()).extracting("lastName").containsExactly("Albrecht", "Zimmermann");
        assertThat(service.listByProperty(owner.id(), "property-1")).hasSize(1);
        assertThat(service.listByProperty(owner.id(), "property-1").get(0).tenants()).hasSize(2);
    }

    @Test
    void listsApartmentsSortedByFloorThenByFirstTenantName() {
        service.create(owner.id(), "property-1", request(3, "Max", "Meier"));
        service.create(owner.id(), "property-1", request(0, "Zoe", "Zander"));
        service.create(owner.id(), "property-1", request(0, "Anna", "Albrecht"));

        assertThat(service.listByProperty(owner.id(), "property-1"))
                .extracting(apartment -> apartment.tenants().get(0).lastName())
                .containsExactly("Albrecht", "Zander", "Meier");
    }

    @Test
    void updatesTheTenantAndTheSharedApartmentData() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Max", "Mustermann"));
        String tenantId = created.tenants().get(0).id();

        ApartmentResponse updated = service.updateTenant(owner.id(), tenantId, new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(
                        3, new BigDecimal("70.00"), new BigDecimal("900.00"), new BigDecimal("700.00"), new BigDecimal("200.00")),
                new TenantDetailsRequest("Erika", "Musterfrau")));

        assertThat(updated.floor()).isEqualTo(3);
        assertThat(updated.areaSquareMeters()).isEqualByComparingTo("70.00");
        assertThat(updated.totalRent()).isEqualByComparingTo("900.00");
        assertThat(updated.tenants()).extracting("lastName").containsExactly("Musterfrau", "Mustermann");
        assertThat(service.listByProperty(owner.id(), "property-1").get(0).floor()).isEqualTo(3);
    }

    @Test
    void deletingANonLastTenantKeepsTheApartment() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        ApartmentResponse withSecond = service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Max", "Zimmermann"));

        service.deleteTenant(owner.id(), withSecond.tenants().get(1).id());

        ApartmentResponse remaining = service.listByProperty(owner.id(), "property-1").get(0);
        assertThat(remaining.tenants()).extracting("lastName").containsExactly("Mustermann");
        assertThat(tenantRepository.count()).isEqualTo(1);
    }

    @Test
    void deletingTheLastTenantAlsoDeletesTheApartment() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        service.deleteTenant(owner.id(), created.tenants().get(0).id());

        assertThat(service.listByProperty(owner.id(), "property-1")).isEmpty();
        assertThat(apartmentRepository.count()).isZero();
        assertThat(tenantRepository.count()).isZero();
    }

    @Test
    void treatsAnotherAccountsDataAsNotFound() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        String tenantId = created.tenants().get(0).id();

        assertThatThrownBy(() -> service.listByProperty(otherOwner.id(), "property-1"))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.create(otherOwner.id(), "property-1", request(1, "Hans", "Fremd")))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.addTenant(otherOwner.id(), created.id(), new TenantDetailsRequest("Hans", "Fremd")))
                .isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.updateTenant(otherOwner.id(), tenantId, request(1, "Hans", "Fremd")))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.deleteTenant(otherOwner.id(), tenantId)).isInstanceOf(TenantNotFoundException.class);
        assertThat(service.listByProperty(owner.id(), "property-1").get(0).tenants()).hasSize(1);
    }

    @Test
    void throwsForUnknownIds() {
        assertThatThrownBy(() -> service.listByProperty(owner.id(), "unknown")).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.addTenant(owner.id(), "unknown", new TenantDetailsRequest("A", "B")))
                .isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.updateTenant(owner.id(), "unknown", request(1, "A", "B")))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.deleteTenant(owner.id(), "unknown")).isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsAnEleventhTenantInOneApartment() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        for (int index = 1; index < ApartmentService.MAX_TENANTS_PER_APARTMENT; index++) {
            service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Mieter", "Nummer" + index));
        }

        assertThatThrownBy(() -> service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Zu", "Viel")))
                .isInstanceOfSatisfying(TenantLimitExceededException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(TenantLimitExceededException.REASON_TENANT_LIMIT));
    }

    @Test
    void rejectsAnApartmentBeyondThePropertyLimit() {
        for (int index = 0; index < ApartmentService.MAX_APARTMENTS_PER_PROPERTY; index++) {
            apartmentRepository.save(new Apartment(
                    "apartment-" + index, property, 1, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO));
        }

        assertThatThrownBy(() -> service.create(owner.id(), "property-1", request(1, "Zu", "Viel")))
                .isInstanceOfSatisfying(TenantLimitExceededException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(TenantLimitExceededException.REASON_APARTMENT_LIMIT));
    }

    private ApartmentWithTenantRequest request(int floor, String firstName, String lastName) {
        return new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(
                        floor, new BigDecimal("64.50"), new BigDecimal("850.00"), new BigDecimal("650.00"), new BigDecimal("200.00")),
                new TenantDetailsRequest(firstName, lastName));
    }
}
