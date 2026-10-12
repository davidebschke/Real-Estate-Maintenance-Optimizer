package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.remo.realestatemaintainceoptimizer.RentFixtures;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentUpdateRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRentRequestException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.TenantLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRentChangeRepository;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.RentPaymentRepository;
import com.remo.realestatemaintainceoptimizer.repository.TenantRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.stream.IntStream;
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

    @Autowired
    private RentPaymentRepository rentPaymentRepository;

    @Autowired
    private ApartmentRentChangeRepository rentChangeRepository;

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

    private static final YearMonth CURRENT_MONTH = YearMonth.now(ZoneId.of("Europe/Berlin"));

    private ApartmentUpdateRequest rentUpdate(String totalRent, YearMonth effectiveMonth) {
        return new ApartmentUpdateRequest(
                new ApartmentDetailsRequest(2, new BigDecimal("64.50"), new BigDecimal(totalRent), new BigDecimal("650.00"), new BigDecimal("200.00")),
                effectiveMonth);
    }

    @Test
    void recordsAChangedRentFromTheCurrentMonthOnWhenNoMonthIsGiven() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        service.updateApartment(owner.id(), created.id(), rentUpdate("850.00", null));
        assertThat(rentChangeRepository.count()).isZero();

        service.updateApartment(owner.id(), created.id(), rentUpdate("900.00", null));

        assertThat(rentChangeRepository.findAllByApartmentPropertyId("property-1")).singleElement().satisfies(change -> {
            assertThat(change.rent()).isEqualByComparingTo("900.00");
            assertThat(change.effectiveMonth()).isEqualTo(CURRENT_MONTH);
        });
        assertThat(apartmentRepository.findAllByPropertyId("property-1").getFirst().rentBeforeChanges()).isEqualByComparingTo("850.00");
    }

    @Test
    void aChosenEarlierMonthReplacesTheLaterRentChangesAndKeepsTheRentBeforeTheFirstOne() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.updateApartment(owner.id(), created.id(), rentUpdate("900.00", CURRENT_MONTH.minusMonths(1)));
        service.updateApartment(owner.id(), created.id(), rentUpdate("950.00", CURRENT_MONTH));

        service.updateApartment(owner.id(), created.id(), rentUpdate("870.00", CURRENT_MONTH.minusMonths(2)));

        assertThat(rentChangeRepository.findAllByApartmentPropertyId("property-1")).singleElement().satisfies(change -> {
            assertThat(change.rent()).isEqualByComparingTo("870.00");
            assertThat(change.effectiveMonth()).isEqualTo(CURRENT_MONTH.minusMonths(2));
        });
        assertThat(apartmentRepository.findAllByPropertyId("property-1").getFirst().rentBeforeChanges()).isEqualByComparingTo("850.00");
        assertThat(service.listByProperty(owner.id(), "property-1").getFirst().totalRent()).isEqualByComparingTo("870.00");
    }

    @Test
    void ignoresTheChosenMonthWhileTheRentStaysTheSame() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        service.updateApartment(owner.id(), created.id(), rentUpdate("850.00", CURRENT_MONTH.minusMonths(5)));

        assertThat(rentChangeRepository.count()).isZero();
    }

    @Test
    void rejectsARentMonthInTheFutureOrBeforeTheSupportedYears() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        assertThatThrownBy(() -> service.updateApartment(owner.id(), created.id(), rentUpdate("900.00", CURRENT_MONTH.plusMonths(1))))
                .isInstanceOfSatisfying(InvalidRentRequestException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidRentRequestException.REASON_INVALID_RENT_MONTH));
        assertThatThrownBy(() -> service.updateApartment(owner.id(), created.id(), rentUpdate("900.00", YearMonth.of(1999, 12))))
                .isInstanceOf(InvalidRentRequestException.class);
        assertThat(service.listByProperty(owner.id(), "property-1").getFirst().totalRent()).isEqualByComparingTo("850.00");
    }

    @Test
    void createsAVacantApartmentWithoutUsingUpADemoTenantCreation() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0, 1);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo Objekt", "Demostr. 1", "pi-building"));

        ApartmentResponse vacant = service.create(demo.id(), "demo-property", new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(1, new BigDecimal("50.00"), new BigDecimal("600.00"), new BigDecimal("450.00"), new BigDecimal("150.00")),
                null));

        assertThat(vacant.tenants()).isEmpty();
        assertThat(service.listByProperty(demo.id(), "demo-property")).singleElement().satisfies(apartment -> assertThat(apartment.tenants()).isEmpty());
        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingTenantCreations()).isEqualTo(1);
    }

    @Test
    void addingATenantToAVacantApartmentRestartsItsRentJudgmentFromTheCurrentMonth() {
        Apartment vacant = RentFixtures.saveApartment(apartmentRepository, propertyRepository.findById("property-1").orElseThrow(), "vacant-1", "600.00");
        vacant.startRentTracking(CURRENT_MONTH.minusMonths(4));
        apartmentRepository.save(vacant);

        service.addTenant(owner.id(), "vacant-1", new TenantDetailsRequest("Erika", "Mustermann"));
        service.addTenant(owner.id(), "vacant-1", new TenantDetailsRequest("Max", "Mustermann"));

        assertThat(apartmentRepository.findAllByPropertyId("property-1").getFirst().rentTrackingFrom()).isEqualTo(CURRENT_MONTH);
    }

    @Test
    void aDemoAccountCanCreateOnlyAFewApartmentsPerProperty() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0, 5);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo Objekt", "Demostr. 1", "pi-building"));
        ApartmentWithTenantRequest vacant = new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(1, new BigDecimal("50.00"), new BigDecimal("600.00"), new BigDecimal("450.00"), new BigDecimal("150.00")),
                null);
        for (int index = 0; index < ApartmentService.MAX_APARTMENTS_PER_DEMO_PROPERTY; index++) {
            service.create(demo.id(), "demo-property", vacant);
        }

        assertThatThrownBy(() -> service.create(demo.id(), "demo-property", vacant))
                .isInstanceOfSatisfying(TenantLimitExceededException.class,
                        exception -> assertThat(exception.limit()).isEqualTo(ApartmentService.MAX_APARTMENTS_PER_DEMO_PROPERTY));
    }

    @Test
    void aTenantCanBeAddedToAVacantApartment() {
        ApartmentResponse vacant = service.create(owner.id(), "property-1", new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(1, new BigDecimal("50.00"), new BigDecimal("600.00"), new BigDecimal("450.00"), new BigDecimal("150.00")),
                null));

        ApartmentResponse filled = service.addTenant(owner.id(), vacant.id(), new TenantDetailsRequest("Erika", "Mustermann"));

        assertThat(filled.tenants()).hasSize(1);
    }

    @Test
    void deletesAnApartmentWithItsTenantsPaymentsAndRentHistory() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.updateApartment(owner.id(), created.id(), rentUpdate("900.00", CURRENT_MONTH));
        Apartment apartment = apartmentRepository.findAllByPropertyId("property-1").getFirst();
        rentPaymentRepository.save(new RentPayment("payment-1", apartment, apartment.tenants().getFirst(), LocalDate.of(2026, 10, 2),
                new BigDecimal("850.00"), "Miete", 2026, 10));

        service.deleteApartment(owner.id(), created.id());

        assertThat(apartmentRepository.count()).isZero();
        assertThat(tenantRepository.count()).isZero();
        assertThat(rentPaymentRepository.count()).isZero();
        assertThat(rentChangeRepository.count()).isZero();
    }

    @Test
    void deletingATenantKeepsTheirPaymentsNotedAsPaymentsOfTheFormerTenant() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Max", "Mustermann"));
        Apartment apartment = apartmentRepository.findAllByPropertyId("property-1").getFirst();
        Tenant erika = apartment.tenants().stream().filter(tenant -> "Erika".equals(tenant.firstName())).findFirst().orElseThrow();
        Tenant max = apartment.tenants().stream().filter(tenant -> "Max".equals(tenant.firstName())).findFirst().orElseThrow();
        rentPaymentRepository.save(new RentPayment("payment-1", apartment, erika, LocalDate.of(2026, 10, 2), new BigDecimal("850.00"), "Miete", 2026, 10));
        rentPaymentRepository.save(new RentPayment("payment-2", apartment, max, LocalDate.of(2026, 9, 2), new BigDecimal("850.00"), "Miete", 2026, 9));

        service.deleteTenant(owner.id(), erika.id());

        RentPayment formerPayment = rentPaymentRepository.findById("payment-1").orElseThrow();
        assertThat(formerPayment.tenant()).isNull();
        assertThat(formerPayment.formerTenantName()).isEqualTo("Mustermann, Erika");
        assertThat(rentPaymentRepository.sumByApartmentAndMonthForProperty("property-1"))
                .extracting(sum -> sum.getApartmentId()).containsOnly(created.id());
        assertThat(rentPaymentRepository.findById("payment-2").orElseThrow().formerTenantName()).isNull();
    }

    @Test
    void deletingTheLastTenantKeepsTheVacantApartmentAndTheirPaymentsNotedAsFormerTenantPayments() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        Apartment apartment = apartmentRepository.findAllByPropertyId("property-1").getFirst();
        rentPaymentRepository.save(new RentPayment("payment-1", apartment, apartment.tenants().getFirst(), LocalDate.of(2026, 10, 2),
                new BigDecimal("850.00"), "Miete", 2026, 10));

        service.deleteTenant(owner.id(), created.tenants().get(0).id());

        assertThat(service.listByProperty(owner.id(), "property-1")).singleElement()
                .satisfies(remaining -> assertThat(remaining.tenants()).isEmpty());
        assertThat(tenantRepository.count()).isZero();
        RentPayment formerPayment = rentPaymentRepository.findById("payment-1").orElseThrow();
        assertThat(formerPayment.tenant()).isNull();
        assertThat(formerPayment.formerTenantName()).isEqualTo("Mustermann, Erika");
    }

    @Test
    void updatesTheNameOfATenantOnly() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Max", "Mustermann"));
        String tenantId = created.tenants().get(0).id();

        ApartmentResponse updated = service.updateTenant(owner.id(), tenantId, new TenantDetailsRequest("Erika", "Musterfrau"));

        assertThat(updated.floor()).isEqualTo(2);
        assertThat(updated.totalRent()).isEqualByComparingTo("850.00");
        assertThat(updated.tenants()).extracting("lastName").containsExactly("Musterfrau", "Mustermann");
    }

    @Test
    void updatesTheSharedApartmentData() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));
        service.addTenant(owner.id(), created.id(), new TenantDetailsRequest("Max", "Mustermann"));

        ApartmentResponse updated = service.updateApartment(owner.id(), created.id(), new ApartmentUpdateRequest(
                new ApartmentDetailsRequest(
                        3, new BigDecimal("70.00"), new BigDecimal("900.00"), new BigDecimal("700.00"), new BigDecimal("200.00")),
                null));

        assertThat(updated.floor()).isEqualTo(3);
        assertThat(updated.areaSquareMeters()).isEqualByComparingTo("70.00");
        assertThat(updated.totalRent()).isEqualByComparingTo("900.00");
        assertThat(updated.tenants()).hasSize(2);
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
    void deletingTheLastTenantKeepsTheApartment() {
        ApartmentResponse created = service.create(owner.id(), "property-1", request(2, "Erika", "Mustermann"));

        service.deleteTenant(owner.id(), created.tenants().get(0).id());

        assertThat(service.listByProperty(owner.id(), "property-1")).hasSize(1);
        assertThat(apartmentRepository.count()).isEqualTo(1);
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
        assertThatThrownBy(() -> service.updateTenant(otherOwner.id(), tenantId, new TenantDetailsRequest("Hans", "Fremd")))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.updateApartment(otherOwner.id(), created.id(), rentUpdate("900.00", null)))
                .isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.deleteApartment(otherOwner.id(), created.id())).isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.deleteTenant(otherOwner.id(), tenantId)).isInstanceOf(TenantNotFoundException.class);
        assertThat(service.listByProperty(owner.id(), "property-1").get(0).tenants()).hasSize(1);
    }

    @Test
    void throwsForUnknownIds() {
        assertThatThrownBy(() -> service.listByProperty(owner.id(), "unknown")).isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.addTenant(owner.id(), "unknown", new TenantDetailsRequest("A", "B")))
                .isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.updateTenant(owner.id(), "unknown", new TenantDetailsRequest("A", "B")))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.updateApartment(owner.id(), "unknown", rentUpdate("900.00", null)))
                .isInstanceOf(ApartmentNotFoundException.class);
        assertThatThrownBy(() -> service.deleteApartment(owner.id(), "unknown")).isInstanceOf(ApartmentNotFoundException.class);
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
        apartmentRepository.saveAll(IntStream.range(0, ApartmentService.MAX_APARTMENTS_PER_PROPERTY)
                .mapToObj(index -> new Apartment(
                        "apartment-" + index, property, 1, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO))
                .toList());

        assertThatThrownBy(() -> service.create(owner.id(), "property-1", request(1, "Zu", "Viel")))
                .isInstanceOfSatisfying(TenantLimitExceededException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(TenantLimitExceededException.REASON_APARTMENT_LIMIT));
    }

    @Test
    void aDemoAccountCanOnlyCreateAsManyTenantsAsItsRemainingLimit() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0, 2);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo Objekt", "Demostr. 1", "pi-building"));
        ApartmentResponse created = service.create(demo.id(), "demo-property", request(1, "Erika", "Mustermann"));
        service.addTenant(demo.id(), created.id(), new TenantDetailsRequest("Max", "Mustermann"));

        assertThatThrownBy(() -> service.addTenant(demo.id(), created.id(), new TenantDetailsRequest("Zu", "Viel")))
                .isInstanceOf(CreationQuotaExceededException.class)
                .extracting("resource").isEqualTo(CreationQuotaExceededException.RESOURCE_TENANT);
        assertThatThrownBy(() -> service.create(demo.id(), "demo-property", request(2, "Zu", "Viel")))
                .isInstanceOf(CreationQuotaExceededException.class);
        assertThat(service.listByProperty(demo.id(), "demo-property")).hasSize(1);
        assertThat(service.listByProperty(demo.id(), "demo-property").get(0).tenants()).hasSize(2);
        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingTenantCreations()).isZero();
    }

    @Test
    void editingAndDeletingTenantsNeverUsesUpTheDemoLimit() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0, 1);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo Objekt", "Demostr. 1", "pi-building"));
        ApartmentResponse created = service.create(demo.id(), "demo-property", request(1, "Erika", "Mustermann"));

        service.updateTenant(demo.id(), created.tenants().get(0).id(), new TenantDetailsRequest("Erika", "Musterfrau"));
        service.deleteTenant(demo.id(), created.tenants().get(0).id());

        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingTenantCreations()).isZero();
    }

    private ApartmentWithTenantRequest request(int floor, String firstName, String lastName) {
        return new ApartmentWithTenantRequest(
                new ApartmentDetailsRequest(
                        floor, new BigDecimal("64.50"), new BigDecimal("850.00"), new BigDecimal("650.00"), new BigDecimal("200.00")),
                new TenantDetailsRequest(firstName, lastName));
    }
}
