package com.remo.realestatemaintainceoptimizer.repository;

/**
 * The number of apartments of one property.
 */
public interface PropertyApartmentCount {

    String getPropertyId();

    long getApartmentCount();
}
