package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a new password violates the password policy, identified by a localizable reason code.
 */
public class InvalidNewPasswordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String REASON_TOO_SHORT = "newPasswordTooShort";
    public static final String REASON_TOO_LONG = "newPasswordTooLong";
    public static final String REASON_UNCHANGED = "newPasswordUnchanged";

    private final String reason;

    public InvalidNewPasswordException(String reasonCode) {
        super(reasonCode);
        this.reason = reasonCode;
    }

    public String reasonCode() {
        return reason;
    }
}
