package com.starktech.diagnostics;

public class DiagnosticsFailedException extends IllegalStateException {
    public DiagnosticsFailedException(String message) {
        super(message);
    }
}
