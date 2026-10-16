package com.starktech.inventory;

public class DroneUnit {
    public enum Status {
        NEW, RESERVED, RELEASED
    }

    private final String id;
    private final String droneId;
    private Status status;

    public DroneUnit(String id, String droneId, Status status) {
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
            (status == Status.NEW && next == Status.RESERVED)
            || (status == Status.RESERVED && next == Status.RELEASED);

        if (!legal) {
            throw new IllegalStateException(
                "Drone unit cannot move from " + status + " to " + next + "."
            );
        }

        status = next;
    }
}
