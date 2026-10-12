package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

/**
 * Payload for editing an apartment, with the month from which a changed total rent applies, defaulting to the current month.
 */
public record ApartmentUpdateRequest(@NotNull @Valid ApartmentDetailsRequest apartment, YearMonth rentEffectiveMonth) {
}
