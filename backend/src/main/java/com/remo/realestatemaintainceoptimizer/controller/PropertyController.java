package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.service.PropertyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only property lookups to the frontend.
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
}
