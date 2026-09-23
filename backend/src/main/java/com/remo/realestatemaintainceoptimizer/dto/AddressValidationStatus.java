package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Outcome of validating whether an entered address combination really exists.
 */
public enum AddressValidationStatus {
    MATCH,
    SUGGESTION,
    NOT_FOUND
}
