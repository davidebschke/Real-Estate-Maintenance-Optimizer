package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.RouteRequest;
import com.remo.realestatemaintainceoptimizer.dto.RouteResponse;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxies road route calculation to openrouteservice so its API key never reaches the browser.
 */
@RestController
@RequestMapping("/api/routes")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Calculates the road route through the given ordered positions of the logged-in account.
     */
    @PostMapping
    public RouteResponse route(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody RouteRequest request) {
        return routingService.route(user.id(), request.coordinates());
    }
}
