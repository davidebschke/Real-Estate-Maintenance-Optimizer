package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a route is requested while routing is deliberately switched off by configuration.
 */
public class RoutingDisabledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoutingDisabledException() {
        super("Routing is disabled (remo.routing.enabled=false)");
    }
}
