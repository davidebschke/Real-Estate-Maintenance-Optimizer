package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an AI optimization is requested while the Claude connection is deliberately switched off by configuration.
 */
public class AiDisabledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiDisabledException() {
        super("The AI optimization is disabled (remo.ai.enabled=false)");
    }
}
