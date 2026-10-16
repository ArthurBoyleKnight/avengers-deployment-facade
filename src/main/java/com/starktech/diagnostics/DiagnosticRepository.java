package com.starktech.diagnostics;

import com.starktech.Storage;
import java.nio.file.Path;
import java.util.Properties;

public class DiagnosticRepository {
    private final Storage storage;

    public DiagnosticRepository(Path dataFolder) {
        storage = new Storage(dataFolder);
    }

    public void save(DiagnosticReport report) {
        Properties data = new Properties();
        data.setProperty("id", report.getId());
        data.setProperty("droneId", report.getDroneId());
        data.setProperty("status", report.getStatus().name());

        storage.save("diagnostics", report.getId(), data);
    }

    public DiagnosticReport find(String id) {
        Properties data = storage.load("diagnostics", id);

        if (data == null) {
            return null;
        }

        return new DiagnosticReport(
            data.getProperty("id"),
            data.getProperty("droneId"),
            DiagnosticReport.Status.valueOf(data.getProperty("status"))
        );
    }

    public void delete(String id) {
        storage.delete("diagnostics", id);
    }
}