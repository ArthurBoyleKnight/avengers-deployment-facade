package com.starktech.clearance;

public class ClearanceDeniedException extends IllegalStateException {
    public ClearanceDeniedException(String message) {
        super(message);
    }
}
