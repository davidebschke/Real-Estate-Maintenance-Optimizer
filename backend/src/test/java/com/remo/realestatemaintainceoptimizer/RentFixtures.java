package com.remo.realestatemaintainceoptimizer;

import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import java.math.BigDecimal;

/**
 * Shared data for the rent payment tests: saved apartments with their tenants.
 */
public final class RentFixtures {

    private RentFixtures() {
    }

    /**
     * A tenant to create, identified by a fixed id so tests can refer to it.
     */
    public record TenantSpec(String id, String firstName, String lastName) {
    }

    /**
     * Saves an apartment with the given total rent on the first floor of the property and adds the given tenants to it.
     */
    public static Apartment saveApartment(
            ApartmentRepository apartmentRepository, Property property, String apartmentId, String totalRent, TenantSpec... tenants) {
        Apartment apartment = new Apartment(
                apartmentId, property, 1, new BigDecimal("60.00"), new BigDecimal(totalRent), new BigDecimal(totalRent), BigDecimal.ZERO);
        for (TenantSpec tenant : tenants) {
            apartment.addTenant(tenant.id(), tenant.firstName(), tenant.lastName());
        }
        return apartmentRepository.save(apartment);
    }
}
