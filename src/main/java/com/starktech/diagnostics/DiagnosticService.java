package com.starktech.diagnostics;

public class DiagnosticService {

    public void process(
        String id,
        String droneId,
        boolean fail,
        DiagnosticRepository repository
    ) {
        if (fail) {
            throw new DiagnosticsFailedException("Drone diagnostics failed.");
        }

        if (droneId.endsWith("-LOWBAT")) {
            throw new DiagnosticsFailedException(
                "Drone diagnostics failed: battery too low."
            );
        }

        DiagnosticReport report = new DiagnosticReport(
            id,
            droneId,
            DiagnosticReport.Status.NEW
        );
        report.transitionTo(DiagnosticReport.Status.PASSED);

        repository.save(report);
    }

    public void reverse(String id, DiagnosticRepository repository) {
        DiagnosticReport report = repository.find(id);

        if (report != null) {
            report.transitionTo(DiagnosticReport.Status.REVERSED);
            repository.save(report);
        }
    }
}
