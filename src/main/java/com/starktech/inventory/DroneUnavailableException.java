package com.starktech.inventory;

public class DroneUnavailableException extends IllegalStateException {
    public DroneUnavailableException(String message) {
        super(message);
    }
}
