package com.remo.realestatemaintainceoptimizer.repository;

import java.math.BigDecimal;

/**
 * The total rent paid for one apartment in one calendar month, as aggregated by {@link RentPaymentRepository}.
 */
public interface ApartmentMonthPayment {

    String getApartmentId();

    int getPeriodYear();

    int getPeriodMonth();

    BigDecimal getTotalAmount();
}
