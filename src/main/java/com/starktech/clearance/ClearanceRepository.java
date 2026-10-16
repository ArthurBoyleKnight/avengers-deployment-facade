package com.starktech.clearance;

import com.starktech.Storage;
import java.nio.file.Path;
import java.util.Properties;

public class ClearanceRepository {
    private final Storage storage;

    public ClearanceRepository(Path dataFolder) {
        storage = new Storage(dataFolder);
    }

    public void save(MissionClearance clearance) {
        Properties data = new Properties();
        data.setProperty("id", clearance.getId());
        data.setProperty("missionCode", clearance.getMissionCode());
        data.setProperty("status", clearance.getStatus().name());

        storage.save("clearances", clearance.getId(), data);
    }

    public MissionClearance find(String id) {
        Properties data = storage.load("clearances", id);

        if (data == null) {
            return null;
        }

        return new MissionClearance(
            data.getProperty("id"),
            data.getProperty("missionCode"),
            MissionClearance.Status.valueOf(data.getProperty("status"))
        );
    }

    public void delete(String id) {
        storage.delete("clearances", id);
    }
}