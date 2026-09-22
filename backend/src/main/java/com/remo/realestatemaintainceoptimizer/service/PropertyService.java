package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Business logic for creating and looking up properties.
 */
@Service
public class PropertyService {

    static final String DEFAULT_ICON = "pi-building";

    private final PropertyFileRepository repository;

    public PropertyService(PropertyFileRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns every property, sorted by name.
     */
    public List<PropertyResponse> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Property::name))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the property with the given id.
     */
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

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.id(), property.name(), property.address(), property.icon(),
                property.latitude(), property.longitude());
    }
}
