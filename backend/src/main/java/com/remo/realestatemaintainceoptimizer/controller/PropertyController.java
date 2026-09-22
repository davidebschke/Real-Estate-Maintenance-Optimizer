package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.service.PropertyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Exposes property create and read lookups to the frontend.
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    /**
     * Returns every property, sorted by name.
     */
    @GetMapping
    public List<PropertyResponse> listProperties() {
        return propertyService.listAll();
    }

    /**
     * Returns the property with the given id.
     */
    @GetMapping("/{id}")
    public PropertyResponse getProperty(@PathVariable String id) {
        return propertyService.getById(id);
    }

    /**
     * Creates a new property.
     */
    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody CreatePropertyRequest request) {
        PropertyResponse created = propertyService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
