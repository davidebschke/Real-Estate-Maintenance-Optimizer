package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyTenantCount;
import com.remo.realestatemaintainceoptimizer.repository.TenantRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating, looking up, updating and deleting the properties of one account; another account's properties behave as if they did not exist.
 */
@Service
@Transactional
public class PropertyService {

    static final String DEFAULT_ICON = "pi-building";

    private final PropertyRepository repository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public PropertyService(
            PropertyRepository repository, UserRepository userRepository, TenantRepository tenantRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Returns every property of the given account, sorted by name.
     */
    @Transactional(readOnly = true)
    public List<PropertyResponse> listAll(String ownerId) {
        Map<String, Long> tenantCounts = tenantRepository.countTenantsPerPropertyByOwnerId(ownerId).stream()
                .collect(Collectors.toMap(PropertyTenantCount::getPropertyId, PropertyTenantCount::getTenantCount));
        return repository.findAllByOwnerIdOrderByNameAsc(ownerId).stream()
                .map(property -> toResponse(property, tenantCounts.getOrDefault(property.id(), 0L)))
                .toList();
    }

    /**
     * Returns the property with the given id of the given account.
     */
    @Transactional(readOnly = true)
    public PropertyResponse getById(String ownerId, String id) {
        return toResponse(loadOrThrow(ownerId, id));
    }

    /**
     * Creates a new property for the given account with a generated id and the default icon, using up one of its creations if it has a limit.
     */
    public PropertyResponse create(String ownerId, CreatePropertyRequest request) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new AccountNotFoundException(ownerId))
                .consumePropertyCreation();
        Property property = new Property(
                UUID.randomUUID().toString(),
                ownerId,
                request.name(),
                request.address(),
                DEFAULT_ICON,
                request.latitude(),
                request.longitude());
        return toResponse(repository.save(property));
    }

    /**
     * Updates the name, address and coordinates of the property with the given id, keeping its id and icon; appointments reference it and therefore reflect the change automatically.
     */
    public PropertyResponse update(String ownerId, String id, CreatePropertyRequest request) {
        Property property = loadOrThrow(ownerId, id);
        property.updateDetails(request.name(), request.address(), request.latitude(), request.longitude());
        return toResponse(property);
    }

    /**
     * Deletes the property with the given id along with every appointment referencing it, cascaded by the database.
     */
    public void delete(String ownerId, String id) {
        repository.delete(loadOrThrow(ownerId, id));
    }

    private Property loadOrThrow(String ownerId, String id) {
        return repository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new PropertyNotFoundException(id));
    }

    private PropertyResponse toResponse(Property property) {
        return toResponse(property, tenantRepository.countByApartmentPropertyId(property.id()));
    }

    private PropertyResponse toResponse(Property property, long tenantCount) {
        return new PropertyResponse(
                property.id(), property.name(), property.address(), property.icon(),
                property.latitude(), property.longitude(), tenantCount);
    }
}
