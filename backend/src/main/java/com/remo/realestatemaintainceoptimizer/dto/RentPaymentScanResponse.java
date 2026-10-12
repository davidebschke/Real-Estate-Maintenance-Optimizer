package com.remo.realestatemaintainceoptimizer.dto;

import java.util.List;

/**
 * The payments found on a scanned bank statement, awaiting the user's confirmation.
 */
public record RentPaymentScanResponse(List<ScannedPaymentResponse> payments) {
}
