package com.remo.realestatemaintainceoptimizer.controller;

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
}
