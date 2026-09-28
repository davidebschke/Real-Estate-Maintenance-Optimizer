package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating, looking up, updating and deleting properties.
 */
@Service
@Transactional
public class PropertyService {

    static final String DEFAULT_ICON = "pi-building";

    private final PropertyRepository repository;

    public PropertyService(PropertyRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns every property, sorted by name.
     */
    @Transactional(readOnly = true)
    public List<PropertyResponse> listAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the property with the given id.
     */
    @Transactional(readOnly = true)
    public PropertyResponse getById(String id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id)));
    }

    /**
     * Creates a new property with a generated id and the default icon.
     */
    public PropertyResponse create(CreatePropertyRequest request) {
        Property property = new Property(
                UUID.randomUUID().toString(),
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
    public PropertyResponse update(String id, CreatePropertyRequest request) {
        Property property = repository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id));
        property.updateDetails(request.name(), request.address(), request.latitude(), request.longitude());
        return toResponse(property);
    }

    /**
     * Deletes the property with the given id along with every appointment referencing it, cascaded by the database.
     */
    public void delete(String id) {
        Property property = repository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id));
        repository.delete(property);
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.id(), property.name(), property.address(), property.icon(),
                property.latitude(), property.longitude());
    }
}
