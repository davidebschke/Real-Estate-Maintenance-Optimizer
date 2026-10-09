package com.remo.realestatemaintainceoptimizer.repository;

/**
 * The number of tenants living in the apartments of one property.
 */
public interface PropertyTenantCount {

    String getPropertyId();

    long getTenantCount();
}
