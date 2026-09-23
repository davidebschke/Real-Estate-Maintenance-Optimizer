package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Result of validating an entered address, with a coordinate pair for a {@link AddressValidationStatus#MATCH} and a
 * corrected address for a {@link AddressValidationStatus#SUGGESTION}, both null otherwise.
 */
public record AddressValidationResponse(
        AddressValidationStatus status,
        Double latitude,
        Double longitude,
        String suggestedStreet,
        String suggestedHouseNumber,
        String suggestedPostalCode,
        String suggestedCity) {

    public static AddressValidationResponse match(double latitude, double longitude) {
        return new AddressValidationResponse(AddressValidationStatus.MATCH, latitude, longitude, null, null, null, null);
    }

    public static AddressValidationResponse suggestion(
            String suggestedStreet, String suggestedHouseNumber, String suggestedPostalCode, String suggestedCity) {
        return new AddressValidationResponse(
                AddressValidationStatus.SUGGESTION,
                null,
                null,
                suggestedStreet,
                suggestedHouseNumber,
                suggestedPostalCode,
                suggestedCity);
    }

    public static AddressValidationResponse notFound() {
        return new AddressValidationResponse(AddressValidationStatus.NOT_FOUND, null, null, null, null, null, null);
    }
}
