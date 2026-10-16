package com.starktech.facade;

public class DeploymentNotFoundException extends IllegalArgumentException {
    public DeploymentNotFoundException(String id) {
        super("Deployment not found.");
    }
}
