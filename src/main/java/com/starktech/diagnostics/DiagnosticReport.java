package com.starktech.diagnostics;

public class DiagnosticReport {
    public enum Status {
        NEW, PASSED, REVERSED
    }

    private final String id;
    private final String droneId;
    private Status status;

    public DiagnosticReport(String id, String droneId, Status status) {
        this.id = id;
        this.droneId = droneId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getDroneId() {
        return droneId;
    }

    public Status getStatus() {
        return status;
    }

    public void transitionTo(Status next) {
        boolean legal =
            (status == Status.NEW && next == Status.PASSED)
            || (status == Status.PASSED && next == Status.REVERSED);

        if (!legal) {
            throw new IllegalStateException(
                "Diagnostic report cannot move from " + status + " to " + next + "."
            );
        }

        status = next;
    }
}
