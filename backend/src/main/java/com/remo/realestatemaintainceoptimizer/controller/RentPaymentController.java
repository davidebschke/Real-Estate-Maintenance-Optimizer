package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.RentAlertResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentPaymentScanResponse;
import com.remo.realestatemaintainceoptimizer.dto.RentYearResponse;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsRequest;
import com.remo.realestatemaintainceoptimizer.dto.SaveRentPaymentsResponse;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.service.RentPaymentScanService;
import com.remo.realestatemaintainceoptimizer.service.RentPaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Exposes the logged-in account's rent payment check: scanning a bank statement, storing the confirmed payments, the yearly payment table of a property and the alerts for overdue rent.
 */
@RestController
@RequestMapping("/api")
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;
    private final RentPaymentScanService scanService;

    public RentPaymentController(RentPaymentService rentPaymentService, RentPaymentScanService scanService) {
        this.rentPaymentService = rentPaymentService;
        this.scanService = scanService;
    }

    /**
     * Scans the uploaded bank statement PDF of the given property for rent payments without storing anything.
     */
    @PostMapping(value = "/properties/{propertyId}/rent-payments/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RentPaymentScanResponse scanStatement(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String propertyId,
            @RequestParam("file") MultipartFile file) {
        return scanService.scan(user.id(), propertyId, file);
    }

    /**
     * Stores the payments the user confirmed after a scan.
     */
    @PostMapping("/properties/{propertyId}/rent-payments")
    public ResponseEntity<SaveRentPaymentsResponse> savePayments(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String propertyId,
            @Valid @RequestBody SaveRentPaymentsRequest request) {
        SaveRentPaymentsResponse saved = rentPaymentService.save(user.id(), propertyId, request);
        return ResponseEntity.created(paymentsLocation(propertyId)).body(saved);
    }

    /**
     * Returns the yearly rent payment table of the given property, for the current year unless another is requested.
     */
    @GetMapping("/properties/{propertyId}/rent-payments")
    public RentYearResponse yearOverview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String propertyId,
            @RequestParam(required = false) Integer year) {
        return rentPaymentService.yearOverview(user.id(), propertyId, year);
    }

    /**
     * Returns every tenant of the account whose rent is overdue or still awaited.
     */
    @GetMapping("/rent-payments/alerts")
    public List<RentAlertResponse> alerts(@AuthenticationPrincipal AuthenticatedUser user) {
        return rentPaymentService.alerts(user.id());
    }

    private URI paymentsLocation(String propertyId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/properties/{propertyId}/rent-payments")
                .buildAndExpand(propertyId)
                .toUri();
    }
}
