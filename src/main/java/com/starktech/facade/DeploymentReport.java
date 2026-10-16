package com.starktech.facade;

public record DeploymentReport(
    String id,
    String chain,
    String inventory,
    String diagnostics,
    String clearance,
    String detail
) {
}
