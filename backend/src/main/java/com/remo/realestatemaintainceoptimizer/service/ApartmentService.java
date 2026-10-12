package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentUpdateRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantResponse;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
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
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the apartments of a property and their tenants of one account; another account's properties, apartments and tenants behave as if they did not exist.
 */
@Service
@Transactional
@EnableConfigurationProperties(PaymentProperties.class)
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
    private final RentPaymentRepository rentPaymentRepository;
    private final ApartmentRentChangeRepository rentChangeRepository;
    private final PaymentProperties paymentProperties;

    public ApartmentService(
            ApartmentRepository apartmentRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            RentPaymentRepository rentPaymentRepository,
            ApartmentRentChangeRepository rentChangeRepository,
            PaymentProperties paymentProperties) {
        this.apartmentRepository = apartmentRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.rentChangeRepository = rentChangeRepository;
        this.paymentProperties = paymentProperties;
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
     * Creates a new apartment in the given property, vacant or together with its first tenant, rejecting it when the property already has the maximum number of apartments or a demo account used up its tenant creations.
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
        apartment.startRentTracking(YearMonth.now(paymentProperties.zone()));
        if (request.tenant() != null) {
            addTenantTo(ownerId, apartment, request.tenant());
        }
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
     * Updates the name of the given tenant.
     */
    public ApartmentResponse updateTenant(String ownerId, String tenantId, TenantDetailsRequest request) {
        Tenant tenant = loadTenantOrThrow(ownerId, tenantId);
        tenant.updateName(request.firstName(), request.lastName());
        return toResponse(tenant.apartment());
    }

    /**
     * Updates the data of the given apartment, shared by all its tenants; a changed total rent applies from the requested month on, or from the current month if none is given, replacing every later rent change, while earlier months keep their rent; the apartment is locked meanwhile so concurrent edits cannot mix up the rent history.
     */
    public ApartmentResponse updateApartment(String ownerId, String apartmentId, ApartmentUpdateRequest request) {
        Apartment apartment = apartmentRepository.findByIdAndOwnerIdForUpdate(apartmentId, ownerId)
                .orElseThrow(() -> new ApartmentNotFoundException(apartmentId));
        YearMonth currentMonth = YearMonth.now(paymentProperties.zone());
        YearMonth effectiveMonth = request.rentEffectiveMonth() == null ? currentMonth : request.rentEffectiveMonth();
        if (effectiveMonth.isAfter(currentMonth) || effectiveMonth.getYear() < RentPaymentRequest.FIRST_YEAR) {
            throw new InvalidRentRequestException(InvalidRentRequestException.REASON_INVALID_RENT_MONTH);
        }
        ApartmentDetailsRequest details = request.apartment();
        BigDecimal previousRent = apartment.totalRent();
        apartment.updateDetails(
                details.floor(),
                details.areaSquareMeters(),
                details.totalRent(),
                details.coldRent(),
                details.additionalCosts());
        recordRentChange(apartment, previousRent, effectiveMonth);
        return toResponse(apartment);
    }

    /**
     * Deletes the given apartment together with its tenants, their payments and its rent history; the apartment is locked meanwhile, and unwrapped from its lazy proxy because Spring Data would mistake that for a new entity and skip the delete.
     */
    public void deleteApartment(String ownerId, String apartmentId) {
        Apartment apartment = Hibernate.unproxy(
                apartmentRepository.findByIdAndOwnerIdForUpdate(apartmentId, ownerId)
                        .orElseThrow(() -> new ApartmentNotFoundException(apartmentId)),
                Apartment.class);
        apartmentRepository.delete(apartment);
    }

    /**
     * Deletes the given tenant and notes their name on their payments, which stay with the apartment; the apartment stays too, even without a tenant, and is locked meanwhile so concurrent changes of its tenants run one after another, and unwrapped from its lazy proxy.
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
        rentPaymentRepository.markTenantAsFormer(tenantId, tenant.lastName() + ", " + tenant.firstName());
        apartment.removeTenant(tenant);
    }

    /**
     * Records the apartment's new total rent as valid from the given month on, remembering the rent before its first change and dropping every rent change from that month on, so earlier months keep their rent; nothing is recorded while the rent is unchanged.
     */
    private void recordRentChange(Apartment apartment, BigDecimal previousRent, YearMonth effectiveMonth) {
        if (previousRent.compareTo(apartment.totalRent()) == 0) {
            return;
        }
        apartment.rememberInitialRent(previousRent);
        rentChangeRepository.deleteFrom(apartment.id(), effectiveMonth.atDay(1));
        rentChangeRepository.save(new ApartmentRentChange(UUID.randomUUID().toString(), apartment, effectiveMonth, apartment.totalRent()));
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
