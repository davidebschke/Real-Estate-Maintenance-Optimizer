package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Read-only business logic for looking up properties.
 */
@Service
public class PropertyService {

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

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.id(), property.name(), property.address(), property.icon(),
                property.latitude(), property.longitude());
    }
}
