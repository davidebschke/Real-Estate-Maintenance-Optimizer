package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Payload for the confirmed payments of a scanned bank statement.
 */
public record SaveRentPaymentsRequest(
        @NotEmpty @Size(max = SaveRentPaymentsRequest.MAX_PAYMENTS) List<@Valid RentPaymentRequest> payments) {

    public static final int MAX_PAYMENTS = 500;
}
