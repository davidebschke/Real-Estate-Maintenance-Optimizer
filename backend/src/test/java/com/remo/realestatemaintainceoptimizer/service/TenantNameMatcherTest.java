package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.dto.TenantMatch;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.service.TenantNameMatcher.Assignment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that payer names are assigned to tenants only when the name parts match, regardless of order, case, accents and initials, and that an ambiguous name across apartments stays unassigned.
 */
class TenantNameMatcherTest {

    private final TenantNameMatcher matcher = new TenantNameMatcher();

    private static Apartment apartment(String id) {
        return new Apartment(id, null, 1, new BigDecimal("60"), new BigDecimal("800"), new BigDecimal("600"), new BigDecimal("200"));
    }

    private static Tenant tenant(Apartment apartment, String id, String firstName, String lastName) {
        return apartment.addTenant(id, firstName, lastName);
    }

    @Test
    void assignsAFullNameInAnyOrderAndWritingAsAMatch() {
        Tenant erika = tenant(apartment("a1"), "t1", "Erika", "Müller");
        List<Tenant> tenants = List.of(erika, tenant(apartment("a2"), "t2", "Max", "Schmidt"));

        for (String payer : List.of("Erika Müller", "MÜLLER, ERIKA", "Müller Erika", "erika muller", "E. Müller")) {
            Assignment assignment = matcher.assign(payer, tenants);

            assertThat(assignment.tenant()).as(payer).isSameAs(erika);
            assertThat(assignment.match()).as(payer).isEqualTo(TenantMatch.MATCH);
        }
    }

    @Test
    void aLastNameAloneIsOnlyAPossibleMatch() {
        Tenant erika = tenant(apartment("a1"), "t1", "Erika", "Mustermann");

        Assignment assignment = matcher.assign("Familie Mustermann", List.of(erika));

        assertThat(assignment.tenant()).isSameAs(erika);
        assertThat(assignment.match()).isEqualTo(TenantMatch.POSSIBLE);
    }

    @Test
    void aFullNameWinsOverAnotherTenantWithTheSameLastName() {
        Tenant erika = tenant(apartment("a1"), "t1", "Erika", "Mustermann");
        Tenant max = tenant(apartment("a2"), "t2", "Max", "Mustermann");

        Assignment assignment = matcher.assign("Max Mustermann", List.of(erika, max));

        assertThat(assignment.tenant()).isSameAs(max);
        assertThat(assignment.match()).isEqualTo(TenantMatch.MATCH);
    }

    @Test
    void anAmbiguousLastNameAcrossApartmentsStaysUnassigned() {
        List<Tenant> tenants = List.of(
                tenant(apartment("a1"), "t1", "Erika", "Mustermann"), tenant(apartment("a2"), "t2", "Max", "Mustermann"));

        Assignment assignment = matcher.assign("Familie Mustermann", tenants);

        assertThat(assignment.tenant()).isNull();
        assertThat(assignment.match()).isEqualTo(TenantMatch.NONE);
    }

    @Test
    void coTenantsOfOneApartmentPickTheFirstSinceTheirRentIsShared() {
        Apartment shared = apartment("a1");
        Tenant erika = tenant(shared, "t1", "Erika", "Mustermann");
        Tenant max = tenant(shared, "t2", "Max", "Mustermann");

        Assignment assignment = matcher.assign("Max und Erika Mustermann", List.of(erika, max));

        assertThat(assignment.tenant()).isSameAs(erika);
        assertThat(assignment.match()).isEqualTo(TenantMatch.MATCH);
    }

    @Test
    void coTenantsAreChosenIndependentlyOfTheirOrderInTheList() {
        Apartment shared = apartment("a1");
        Tenant later = tenant(shared, "t2", "Max", "Mustermann");
        Tenant earlier = tenant(shared, "t1", "Erika", "Mustermann");

        assertThat(matcher.assign("Familie Mustermann", List.of(later, earlier)).tenant()).isSameAs(earlier);
        assertThat(matcher.assign("Familie Mustermann", List.of(earlier, later)).tenant()).isSameAs(earlier);
    }

    @Test
    void aMultiWordLastNameNeedsAllItsParts() {
        Tenant tenant = tenant(apartment("a1"), "t1", "Anna", "von Berg");

        assertThat(matcher.assign("Anna von Berg", List.of(tenant)).match()).isEqualTo(TenantMatch.MATCH);
        assertThat(matcher.assign("Anna Berg", List.of(tenant)).match()).isEqualTo(TenantMatch.NONE);
    }

    @Test
    void anUnknownEmptyOrMissingPayerNameStaysUnassigned() {
        List<Tenant> tenants = List.of(tenant(apartment("a1"), "t1", "Erika", "Mustermann"));

        assertThat(matcher.assign("Stadtwerke Aachen", tenants).match()).isEqualTo(TenantMatch.NONE);
        assertThat(matcher.assign("", tenants).match()).isEqualTo(TenantMatch.NONE);
        assertThat(matcher.assign(null, tenants).match()).isEqualTo(TenantMatch.NONE);
        assertThat(matcher.assign("Erika Mustermann", List.of()).tenant()).isNull();
    }
}
