package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.ApartmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentUpdateRequest;
import com.remo.realestatemaintainceoptimizer.dto.ApartmentWithTenantRequest;
import com.remo.realestatemaintainceoptimizer.dto.TenantDetailsRequest;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.service.ApartmentService;
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
 * Exposes the logged-in account's apartments of a property and their tenants for create, read, update and delete to the frontend.
 */
@RestController
@RequestMapping("/api")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    /**
     * Returns every apartment of the given property with its tenants.
     */
    @GetMapping("/properties/{propertyId}/apartments")
    public List<ApartmentResponse> listApartments(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String propertyId) {
        return apartmentService.listByProperty(user.id(), propertyId);
    }

    /**
     * Creates a new apartment in the given property, vacant or together with its first tenant.
     */
    @PostMapping("/properties/{propertyId}/apartments")
    public ResponseEntity<ApartmentResponse> createApartment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String propertyId,
            @Valid @RequestBody ApartmentWithTenantRequest request) {
        ApartmentResponse created = apartmentService.create(user.id(), propertyId, request);
        return ResponseEntity.created(apartmentsLocation(propertyId)).body(created);
    }

    /**
     * Adds a further tenant to the given apartment.
     */
    @PostMapping("/apartments/{apartmentId}/tenants")
    public ResponseEntity<ApartmentResponse> addTenant(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String apartmentId,
            @Valid @RequestBody TenantDetailsRequest request) {
        ApartmentResponse updated = apartmentService.addTenant(user.id(), apartmentId, request);
        return ResponseEntity.created(apartmentsLocation(updated.propertyId())).body(updated);
    }

    /**
     * Updates the name of the given tenant.
     */
    @PutMapping("/tenants/{tenantId}")
    public ApartmentResponse updateTenant(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String tenantId,
            @Valid @RequestBody TenantDetailsRequest request) {
        return apartmentService.updateTenant(user.id(), tenantId, request);
    }

    /**
     * Updates the data of the given apartment, a changed total rent applying from the requested month on.
     */
    @PutMapping("/apartments/{apartmentId}")
    public ApartmentResponse updateApartment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String apartmentId,
            @Valid @RequestBody ApartmentUpdateRequest request) {
        return apartmentService.updateApartment(user.id(), apartmentId, request);
    }

    /**
     * Deletes the given apartment together with its tenants and their payments.
     */
    @DeleteMapping("/apartments/{apartmentId}")
    public ResponseEntity<Void> deleteApartment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String apartmentId) {
        apartmentService.deleteApartment(user.id(), apartmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the given tenant, the apartment staying even without a tenant.
     */
    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String tenantId) {
        apartmentService.deleteTenant(user.id(), tenantId);
        return ResponseEntity.noContent().build();
    }

    private URI apartmentsLocation(String propertyId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/properties/{propertyId}/apartments")
                .buildAndExpand(propertyId)
                .toUri();
    }
}
