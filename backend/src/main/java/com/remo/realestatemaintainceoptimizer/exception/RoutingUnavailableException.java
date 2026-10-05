package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a road route cannot be calculated because the external routing service failed or answered unusably.
 */
public class RoutingUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoutingUnavailableException(String message) {
        super(message);
    }

    public RoutingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
