package com.starktech.inventory;

import com.starktech.Storage;
import java.nio.file.Path;
import java.util.Properties;

public class InventoryRepository {
    private static final String FOLDER = "inventory";

    private final Storage storage;

    public InventoryRepository(Path dataFolder) {
        storage = new Storage(dataFolder);
    }

    public void save(DroneUnit drone) {
        Properties data = new Properties();
        data.setProperty("id", drone.getId());
        data.setProperty("droneId", drone.getDroneId());
        data.setProperty("status", drone.getStatus().name());

        storage.save(FOLDER, drone.getId(), data);
    }

    public DroneUnit find(String id) {
        Properties data = storage.load(FOLDER, id);

        if (data == null) {
            return null;
        }

        return new DroneUnit(
            data.getProperty("id"),
            data.getProperty("droneId"),
            DroneUnit.Status.valueOf(data.getProperty("status"))
        );
    }

    public boolean isDroneReserved(String droneId) {
        for (String id : storage.list(FOLDER)) {
            DroneUnit unit = find(id);

            if (unit.getStatus() == DroneUnit.Status.RESERVED
                && droneId.equals(unit.getDroneId())) {
                return true;
            }
        }

        return false;
    }

    public void delete(String id) {
        storage.delete(FOLDER, id);
    }
}
