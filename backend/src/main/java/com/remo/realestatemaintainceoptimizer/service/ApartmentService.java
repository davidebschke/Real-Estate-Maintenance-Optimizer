package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.ApartmentDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.TenantLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.TenantRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the apartments of a property and their tenants of one account; another account's properties, apartments and tenants behave as if they did not exist.
 */
@Service
@Transactional
public class ApartmentService {

    static final int MAX_APARTMENTS_PER_PROPERTY = 500;
    static final int MAX_TENANTS_PER_APARTMENT = 10;

    private static final Comparator<TenantResponse> TENANT_ORDER = Comparator
            .comparing(TenantResponse::lastName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TenantResponse::firstName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TenantResponse::id);

    private static final Comparator<ApartmentResponse> APARTMENT_ORDER = Comparator
            .comparingInt(ApartmentResponse::floor)
            .thenComparing(ApartmentService::firstTenantLastName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ApartmentResponse::id);

    private final ApartmentRepository apartmentRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public ApartmentService(
            ApartmentRepository apartmentRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository) {
        this.apartmentRepository = apartmentRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns every apartment of the given property with its tenants, sorted by floor and then by the name of its first tenant.
     */
    @Transactional(readOnly = true)
    public List<ApartmentResponse> listByProperty(String ownerId, String propertyId) {
        loadPropertyOrThrow(ownerId, propertyId);
        return apartmentRepository.findAllByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .sorted(APARTMENT_ORDER)
                .toList();
    }

    /**
     * Creates a new apartment in the given property together with its first tenant, rejecting it when the property already has the maximum number of apartments or a demo account used up its tenant creations.
     */
    public ApartmentResponse create(String ownerId, String propertyId, ApartmentWithTenantRequest request) {
        Property property = loadPropertyOrThrow(ownerId, propertyId);
        if (apartmentRepository.countByPropertyId(propertyId) >= MAX_APARTMENTS_PER_PROPERTY) {
            throw new TenantLimitExceededException(
                    TenantLimitExceededException.REASON_APARTMENT_LIMIT, MAX_APARTMENTS_PER_PROPERTY);
        }
        ApartmentDetailsRequest details = request.apartment();
        Apartment apartment = new Apartment(
                UUID.randomUUID().toString(),
                property,
                details.floor(),
                details.areaSquareMeters(),
                details.totalRent(),
                details.coldRent(),
                details.additionalCosts());
        addTenantTo(ownerId, apartment, request.tenant());
        return toResponse(apartmentRepository.save(apartment));
    }

    /**
     * Adds a further tenant to an existing apartment, rejecting it when the apartment already has the maximum number of tenants or a demo account used up its tenant creations; the apartment is locked meanwhile so concurrent additions cannot exceed that maximum together.
     */
    public ApartmentResponse addTenant(String ownerId, String apartmentId, TenantDetailsRequest request) {
        Apartment apartment = apartmentRepository.findByIdAndOwnerIdForUpdate(apartmentId, ownerId)
                .orElseThrow(() -> new ApartmentNotFoundException(apartmentId));
        if (apartment.tenants().size() >= MAX_TENANTS_PER_APARTMENT) {
            throw new TenantLimitExceededException(
                    TenantLimitExceededException.REASON_TENANT_LIMIT, MAX_TENANTS_PER_APARTMENT);
        }
        addTenantTo(ownerId, apartment, request);
        return toResponse(apartment);
    }

    /**
     * Updates the name of the given tenant together with the data of their apartment, which is shared with every other tenant of it.
     */
    public ApartmentResponse updateTenant(String ownerId, String tenantId, ApartmentWithTenantRequest request) {
        Tenant tenant = loadTenantOrThrow(ownerId, tenantId);
        tenant.updateName(request.tenant().firstName(), request.tenant().lastName());
        ApartmentDetailsRequest details = request.apartment();
        Apartment apartment = tenant.apartment();
        apartment.updateDetails(
                details.floor(),
                details.areaSquareMeters(),
                details.totalRent(),
                details.coldRent(),
                details.additionalCosts());
        return toResponse(apartment);
    }

    /**
     * Deletes the given tenant, together with their apartment if they were its last tenant; the apartment is locked meanwhile so concurrent deletions cannot leave it without tenants, and unwrapped from its lazy proxy because Spring Data would mistake that for a new entity and skip the delete.
     */
    public void deleteTenant(String ownerId, String tenantId) {
        String apartmentId = loadTenantOrThrow(ownerId, tenantId).apartment().id();
        Apartment apartment = Hibernate.unproxy(
                apartmentRepository.findByIdAndOwnerIdForUpdate(apartmentId, ownerId)
                        .orElseThrow(() -> new ApartmentNotFoundException(apartmentId)),
                Apartment.class);
        Tenant tenant = apartment.tenants().stream()
                .filter(candidate -> candidate.id().equals(tenantId))
                .findFirst()
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (apartment.hasSingleTenant()) {
            apartmentRepository.delete(apartment);
        } else {
            apartment.removeTenant(tenant);
        }
    }

    private void addTenantTo(String ownerId, Apartment apartment, TenantDetailsRequest request) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new AccountNotFoundException(ownerId))
                .consumeTenantCreation();
        apartment.addTenant(UUID.randomUUID().toString(), request.firstName(), request.lastName());
    }

    private Property loadPropertyOrThrow(String ownerId, String propertyId) {
        return propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }

    private Tenant loadTenantOrThrow(String ownerId, String tenantId) {
        return tenantRepository.findByIdAndApartmentPropertyOwnerId(tenantId, ownerId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private static String firstTenantLastName(ApartmentResponse apartment) {
        return apartment.tenants().isEmpty() ? "" : apartment.tenants().get(0).lastName();
    }

    private ApartmentResponse toResponse(Apartment apartment) {
        List<TenantResponse> tenants = apartment.tenants().stream()
                .map(tenant -> new TenantResponse(tenant.id(), tenant.firstName(), tenant.lastName()))
                .sorted(TENANT_ORDER)
                .toList();
        return new ApartmentResponse(
                apartment.id(),
                apartment.property().id(),
                apartment.floor(),
                apartment.areaSquareMeters(),
                apartment.totalRent(),
                apartment.coldRent(),
                apartment.additionalCosts(),
                tenants);
    }
}
