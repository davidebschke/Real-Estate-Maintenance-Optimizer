package com.remo.realestatemaintainceoptimizer.dto;

/**
 * A tenant as returned to API consumers.
 */
public record TenantResponse(String id, String firstName, String lastName) {
}
