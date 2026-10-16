package com.starktech.facade;

import com.starktech.workflow.ChainRecord;

import java.util.List;

public class AdminFacade {
    private final DeploymentFacade deployment;

    public AdminFacade(DeploymentFacade deployment) {
        this.deployment = deployment;
    }

    public ChainRecord inspect(String id) {
        return deployment.inspect(id);
    }

    public DeploymentReport report(String id) {
        return deployment.report(id);
    }

    public List<ChainRecord> list() {
        return deployment.list();
    }

    public void compensate(String id) {
        deployment.compensate(id);
    }

    public int recoverInterrupted() {
        return deployment.recoverInterrupted();
    }
}
