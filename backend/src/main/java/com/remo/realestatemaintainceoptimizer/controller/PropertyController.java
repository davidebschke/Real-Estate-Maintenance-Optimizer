package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.service.PropertyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Exposes the logged-in account's property create, read, update and delete lookups to the frontend.
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
    public List<PropertyResponse> listProperties(@AuthenticationPrincipal AuthenticatedUser user) {
        return propertyService.listAll(user.id());
    }

    /**
     * Returns the property with the given id.
     */
    @GetMapping("/{id}")
    public PropertyResponse getProperty(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return propertyService.getById(user.id(), id);
    }

    /**
     * Creates a new property.
     */
    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreatePropertyRequest request) {
        PropertyResponse created = propertyService.create(user.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates the name, address and coordinates of the property with the given id.
     */
    @PutMapping("/{id}")
    public PropertyResponse updateProperty(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String id,
            @Valid @RequestBody CreatePropertyRequest request) {
        return propertyService.update(user.id(), id, request);
    }

    /**
     * Deletes the property with the given id along with every appointment referencing it.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        propertyService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
