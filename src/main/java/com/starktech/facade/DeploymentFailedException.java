package com.starktech.facade;

public class DeploymentFailedException extends IllegalStateException {
    private final String chainId;

    public DeploymentFailedException(String chainId, String reason, Throwable cause) {
        super("Deployment failed: " + reason + " (chain ID: " + chainId + ")", cause);
        this.chainId = chainId;
    }

    public String getChainId() {
        return chainId;
    }
}
