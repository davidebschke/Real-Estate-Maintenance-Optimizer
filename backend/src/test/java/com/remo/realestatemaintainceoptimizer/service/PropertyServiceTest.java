package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies property listing (sorted by name), lookup by id, update, cascading delete, account isolation and demo creation limits against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PropertyServiceTest {

    @Autowired
    private PropertyService service;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PropertyRepository repository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    private User otherOwner;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        otherOwner = TestAccounts.saveRegularAccount(userRepository);
    }

    @Test
    void listsEveryPropertyOfTheAccountSortedByName() {
        repository.save(new Property("2", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("1", owner.id(), "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));

        assertThat(service.listAll(owner.id())).extracting("name")
                .containsExactly("Wohnanlage Rheinblick", "Wohnanlage Sonnenhof");
    }

    @Test
    void neverListsThePropertiesOfAnotherAccount() {
        repository.save(new Property("1", otherOwner.id(), "Fremdes Objekt", "Fremdstr. 1", "pi-building"));

        assertThat(service.listAll(owner.id())).isEmpty();
    }

    @Test
    void returnsThePropertyWithTheGivenId() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        assertThat(service.getById(owner.id(), "1").name()).isEqualTo("Wohnanlage Sonnenhof");
    }

    @Test
    void includesStoredCoordinatesInTheResponse() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));

        var response = service.getById(owner.id(), "1");

        assertThat(response.latitude()).isEqualTo(50.94);
        assertThat(response.longitude()).isEqualTo(6.88);
    }

    @Test
    void reportsTheNumberOfTenantsOfEachPropertyWhenListingAndWhenLookingItUp() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        Property withTenants = repository.save(
                new Property("2", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        Apartment apartment = new Apartment(
                "apartment-1", withTenants, 1, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        apartment.addTenant("tenant-1", "Erika", "Mustermann");
        apartment.addTenant("tenant-2", "Max", "Mustermann");
        apartmentRepository.save(apartment);

        assertThat(service.listAll(owner.id()))
                .extracting(PropertyResponse::name, PropertyResponse::tenantCount)
                .containsExactly(tuple("Wohnanlage Rheinblick", 0L), tuple("Wohnanlage Sonnenhof", 2L));
        assertThat(service.getById(owner.id(), "2").tenantCount()).isEqualTo(2);
    }

    @Test
    void aNewlyCreatedPropertyHasNoTenants() {
        var response = service.create(owner.id(), new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3", null, null));

        assertThat(response.tenantCount()).isZero();
    }

    @Test
    void throwsWhenNoPropertyExistsForTheGivenId() {
        assertThatThrownBy(() -> service.getById(owner.id(), "unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void treatsAnotherAccountsPropertyAsNotFoundForReadUpdateAndDelete() {
        repository.save(new Property("1", otherOwner.id(), "Fremdes Objekt", "Fremdstr. 1", "pi-building"));
        CreatePropertyRequest request = new CreatePropertyRequest("Gekapert", "Kaperstr. 1", null, null);

        assertThatThrownBy(() -> service.getById(owner.id(), "1")).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.update(owner.id(), "1", request)).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.delete(owner.id(), "1")).isInstanceOf(PropertyNotFoundException.class);
        assertThat(service.getById(otherOwner.id(), "1").name()).isEqualTo("Fremdes Objekt");
    }

    @Test
    void createsAPropertyWithAGeneratedIdAndTheDefaultIcon() {
        var response = service.create(owner.id(), new CreatePropertyRequest(
                "Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", 50.97, 6.95));

        assertThat(response.id()).isNotBlank();
        assertThat(response.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(response.address()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(response.icon()).isEqualTo("pi-building");
        assertThat(response.latitude()).isEqualTo(50.97);
        assertThat(response.longitude()).isEqualTo(6.95);
        assertThat(service.getById(owner.id(), response.id()).name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(repository.findById(response.id()).orElseThrow().ownerId()).isEqualTo(owner.id());
    }

    @Test
    void createsAPropertyWithoutCoordinatesWhenNoneAreGiven() {
        var response = service.create(owner.id(), new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3", null, null));

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }

    @Test
    void aRegularAccountCanCreateUnlimitedProperties() {
        for (int index = 0; index < 5; index++) {
            service.create(owner.id(), new CreatePropertyRequest("Objekt " + index, "Str. " + index, null, null));
        }

        assertThat(service.listAll(owner.id())).hasSize(5);
    }

    @Test
    void aDemoAccountCanOnlyCreateAsManyPropertiesAsItsRemainingLimit() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 2, 0);
        service.create(demo.id(), new CreatePropertyRequest("Objekt 1", "Str. 1", null, null));
        service.create(demo.id(), new CreatePropertyRequest("Objekt 2", "Str. 2", null, null));

        assertThatThrownBy(() -> service.create(demo.id(), new CreatePropertyRequest("Objekt 3", "Str. 3", null, null)))
                .isInstanceOf(CreationQuotaExceededException.class);
        assertThat(service.listAll(demo.id())).hasSize(2);
        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingPropertyCreations()).isZero();
    }

    @Test
    void creatingForADeletedAccountIsRejected() {
        String deletedId = owner.id();
        userRepository.deleteById(deletedId);

        assertThatThrownBy(() -> service.create(deletedId, new CreatePropertyRequest("Objekt", "Str. 1", null, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updatesNameAddressAndCoordinatesWhileKeepingIdAndIcon() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));

        var response = service.update(owner.id(), "1", new CreatePropertyRequest(
                "Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", 50.97, 6.95));

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(response.address()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(response.icon()).isEqualTo("pi-building");
        assertThat(response.latitude()).isEqualTo(50.97);
        assertThat(response.longitude()).isEqualTo(6.95);
        assertThat(service.getById(owner.id(), "1").name()).isEqualTo("Wohnanlage Nordpark");
    }

    @Test
    void updatingAPropertyIsReflectedInItsAppointmentsOnly() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("2", owner.id(), "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        AppointmentResponse appointment = appointmentService.create(
                owner.id(), createAppointmentRequest("1", LocalDateTime.of(2026, 8, 11, 13, 0)));
        AppointmentResponse unrelated = appointmentService.create(
                owner.id(), createAppointmentRequest("2", LocalDateTime.of(2026, 8, 11, 16, 0)));

        service.update(owner.id(), "1", new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", null, null));

        AppointmentResponse updated = appointmentService.getById(owner.id(), appointment.id());
        assertThat(updated.propertyName()).isEqualTo("Wohnanlage Nordpark");
        assertThat(updated.propertyAddress()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(appointmentService.getById(owner.id(), unrelated.id()).propertyName()).isEqualTo("Wohnanlage Rheinblick");
    }

    @Test
    void throwsWhenUpdatingAnUnknownProperty() {
        assertThatThrownBy(() -> service.update(owner.id(), "unknown", new CreatePropertyRequest("Name", "Address", null, null)))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void deletingAPropertyRemovesIt() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        service.delete(owner.id(), "1");

        assertThatThrownBy(() -> service.getById(owner.id(), "1")).isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void deletingAPropertyAlsoDeletesItsAppointments() {
        repository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("2", owner.id(), "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        AppointmentResponse first = appointmentService.create(
                owner.id(), createAppointmentRequest("1", LocalDateTime.of(2026, 8, 11, 13, 0)));
        AppointmentResponse second = appointmentService.create(
                owner.id(), createAppointmentRequest("1", LocalDateTime.of(2026, 8, 11, 16, 0)));
        AppointmentResponse unrelated = appointmentService.create(
                owner.id(), createAppointmentRequest("2", LocalDateTime.of(2026, 8, 11, 19, 0)));

        service.delete(owner.id(), "1");

        assertThatThrownBy(() -> appointmentService.getById(owner.id(), first.id()))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> appointmentService.getById(owner.id(), second.id()))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThat(appointmentService.getById(owner.id(), unrelated.id())).isNotNull();
    }

    @Test
    void throwsWhenDeletingAnUnknownProperty() {
        assertThatThrownBy(() -> service.delete(owner.id(), "unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    private CreateAppointmentRequest createAppointmentRequest(String propertyId, LocalDateTime start) {
        return new CreateAppointmentRequest(
                "Kellerreinigung Q3",
                propertyId,
                "Was ist zu tun?",
                start,
                120,
                false,
                false,
                null,
                List.of("Kehrmaschine"));
    }
}
