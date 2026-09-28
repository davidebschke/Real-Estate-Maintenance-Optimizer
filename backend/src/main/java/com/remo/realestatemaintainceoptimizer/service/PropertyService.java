package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentFileRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Business logic for creating, looking up, updating and deleting properties.
 */
@Service
public class PropertyService {

    static final String DEFAULT_ICON = "pi-building";

    private final PropertyFileRepository repository;
    private final AppointmentFileRepository appointmentRepository;

    public PropertyService(PropertyFileRepository repository, AppointmentFileRepository appointmentRepository) {
        this.repository = repository;
        this.appointmentRepository = appointmentRepository;
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

    /**
     * Updates the name, address and coordinates of the property with the given id, keeping its id and icon, and propagates the new name/address to every appointment referencing it.
     */
    public PropertyResponse update(String id, CreatePropertyRequest request) {
        Property existing = repository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id));
        Property updated = new Property(
                existing.id(), request.name(), request.address(), existing.icon(),
                request.latitude(), request.longitude());
        Property saved = repository.save(updated);
        appointmentRepository.findByPropertyId(saved.id())
                .forEach(appointment -> appointmentRepository.save(
                        appointment.withPropertyDetails(saved.name(), saved.address())));
        return toResponse(saved);
    }

    /**
     * Deletes the property with the given id along with every appointment referencing it.
     */
    public void delete(String id) {
        Property property = repository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id));
        appointmentRepository.findByPropertyId(property.id())
                .forEach(appointment -> appointmentRepository.deleteById(appointment.id()));
        repository.deleteById(property.id());
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.id(), property.name(), property.address(), property.icon(),
                property.latitude(), property.longitude());
    }
}
