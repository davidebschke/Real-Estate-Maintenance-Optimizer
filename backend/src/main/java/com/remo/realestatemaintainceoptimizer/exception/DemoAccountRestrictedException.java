package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a demo account attempts an action that is reserved for regular accounts, such as changing profile settings.
 */
public class DemoAccountRestrictedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DemoAccountRestrictedException() {
        super("Demo accounts cannot change profile settings");
    }
}
