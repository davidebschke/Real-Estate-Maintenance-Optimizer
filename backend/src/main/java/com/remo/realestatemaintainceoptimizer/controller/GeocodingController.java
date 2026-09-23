package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.AddressValidationResponse;
import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import com.remo.realestatemaintainceoptimizer.service.GeocodingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxies address geocoding to the OpenStreetMap Nominatim API with a server-side identifying User-Agent.
 */
@RestController
@RequestMapping("/api/geocode")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /**
     * Resolves the given address to its coordinates, or null fields if it could not be found.
     */
    @GetMapping
    public GeocodingResponse geocode(@RequestParam String address) {
        return geocodingService.geocode(address);
    }

    /**
     * Checks whether the given address combination really exists, returning a correction suggestion when a unique
     * one can be derived, or a not-found result when the address is ambiguous or entirely unresolvable.
     */
    @GetMapping("/validate")
    public AddressValidationResponse validate(
            @RequestParam String street,
            @RequestParam String houseNumber,
            @RequestParam String postalCode,
            @RequestParam String city) {
        return geocodingService.validateAddress(street, houseNumber, postalCode, city);
    }
}
