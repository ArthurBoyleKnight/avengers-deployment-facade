package com.starktech.inventory;

public class InventoryService {

    public void validateDroneId(String droneId) {
        if (droneId == null
            || !droneId.matches("DRONE-[A-Za-z0-9]+(-[A-Za-z0-9]+)*")) {
            throw new IllegalArgumentException("Invalid drone ID.");
        }
    }

    public void reserve(
        String id,
        String droneId,
        InventoryRepository repository
    ) {
        if (repository.isDroneReserved(droneId)) {
            throw new DroneUnavailableException(
                "Drone " + droneId + " is already reserved."
            );
        }

        DroneUnit drone = new DroneUnit(id, droneId, DroneUnit.Status.NEW);
        drone.transitionTo(DroneUnit.Status.RESERVED);

        repository.save(drone);
    }

    public void release(String id, InventoryRepository repository) {
        DroneUnit drone = repository.find(id);

        if (drone != null) {
            drone.transitionTo(DroneUnit.Status.RELEASED);
            repository.save(drone);
        }
    }
}
