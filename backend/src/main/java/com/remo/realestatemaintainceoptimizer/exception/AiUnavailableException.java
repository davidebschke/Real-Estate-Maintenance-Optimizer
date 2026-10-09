package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when the AI model cannot be reached or answers with something that is not a usable selection.
 */
public class AiUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
