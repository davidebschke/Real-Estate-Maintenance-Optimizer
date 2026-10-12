package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a rent payment request is unusable, identified by a localizable reason code.
 */
public class InvalidRentRequestException extends RuntimeException {

    public static final String REASON_INVALID_YEAR = "invalidYear";
    public static final String REASON_INVALID_RENT_MONTH = "invalidRentMonth";
    public static final String REASON_STATEMENT_EMPTY = "statementEmpty";
    public static final String REASON_STATEMENT_NOT_PDF = "statementNotPdf";
    public static final String REASON_STATEMENT_UNREADABLE = "statementUnreadable";

    private static final long serialVersionUID = 1L;

    private final String code;

    public InvalidRentRequestException(String reasonCode) {
        super(reasonCode);
        this.code = reasonCode;
    }

    public InvalidRentRequestException(String reasonCode, Throwable cause) {
        super(reasonCode, cause);
        this.code = reasonCode;
    }

    public String reasonCode() {
        return code;
    }
}
