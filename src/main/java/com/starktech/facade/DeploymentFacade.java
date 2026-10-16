package com.starktech.facade;

import com.starktech.inventory.*;
import com.starktech.diagnostics.*;
import com.starktech.clearance.*;
import com.starktech.workflow.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class DeploymentFacade {
    private final InventoryRepository inventoryRepo;
    private final DiagnosticRepository diagnosticRepo;
    private final ClearanceRepository clearanceRepo;
    private final ChainRepository chainRepo;

    private final InventoryService inventory = new InventoryService();
    private final DiagnosticService diagnostics = new DiagnosticService();
    private final ClearanceService clearance = new ClearanceService();

    public DeploymentFacade(Path dataFolder) {
        inventoryRepo = new InventoryRepository(dataFolder);
        diagnosticRepo = new DiagnosticRepository(dataFolder);
        clearanceRepo = new ClearanceRepository(dataFolder);
        chainRepo = new ChainRepository(dataFolder);
    }

    public String deploy(
        String droneId,
        String missionCode,
        boolean failB,
        boolean failC
    ) {
        inventory.validateDroneId(droneId);
        clearance.validateMissionCode(missionCode);

        String id = UUID.randomUUID().toString().substring(0, 8);

        ChainRecord chain = new ChainRecord(
            id,
            ChainRecord.Status.STARTED,
            "Drone=" + droneId + ", mission=" + missionCode
        );

        chainRepo.save(chain);

        try {
            inventory.reserve(id, droneId, inventoryRepo);
            advance(chain, ChainRecord.Status.PENDING_B);

            diagnostics.process(id, droneId, failB, diagnosticRepo);
            advance(chain, ChainRecord.Status.PENDING_C);

            clearance.approve(id, missionCode, failC, clearanceRepo);
            advance(chain, ChainRecord.Status.COMPLETED);

            return id;
        } catch (RuntimeException e) {
            rollBack(id);
            throw new DeploymentFailedException(id, e.getMessage(), e);
        }
    }

    private void advance(ChainRecord chain, ChainRecord.Status next) {
        chain.setStatus(next);
        chainRepo.save(chain);
    }

    private void rollBack(String id) {
        ChainRecord chain = chainRepo.find(id);

        if (chain == null) {
            throw new DeploymentNotFoundException(id);
        }

        ChainRecord.Status reached = chain.getStatus();

        if (reached == ChainRecord.Status.COMPLETED) {
            clearance.revoke(id, clearanceRepo);
        }

        if (reached == ChainRecord.Status.COMPLETED
            || reached == ChainRecord.Status.PENDING_C) {
            diagnostics.reverse(id, diagnosticRepo);
        }

        if (reached != ChainRecord.Status.STARTED) {
            inventory.release(id, inventoryRepo);
        }

        if (reached == ChainRecord.Status.STARTED) {
            chain.setStatus(ChainRecord.Status.REJECTED);
            chain.setDetail(chain.getDetail() + "; nothing to undo");
        } else {
            chain.setStatus(ChainRecord.Status.COMPENSATED);
            chain.setDetail(
                chain.getDetail() + "; compensation completed from " + reached
            );
        }

        chainRepo.save(chain);
    }

    public ChainRecord inspect(String id) {
        return chainRepo.find(id);
    }

    public DeploymentReport report(String id) {
        ChainRecord chain = chainRepo.find(id);

        if (chain == null) {
            throw new DeploymentNotFoundException(id);
        }

        DroneUnit drone = inventoryRepo.find(id);
        DiagnosticReport diagnostic = diagnosticRepo.find(id);
        MissionClearance approval = clearanceRepo.find(id);

        return new DeploymentReport(
            id,
            chain.getStatus().name(),
            drone == null ? "NONE" : drone.getStatus().name(),
            diagnostic == null ? "NONE" : diagnostic.getStatus().name(),
            approval == null ? "NONE" : approval.getStatus().name(),
            chain.getDetail()
        );
    }

    public List<ChainRecord> list() {
        return chainRepo.findAll();
    }

    public void compensate(String id) {
        ChainRecord chain = chainRepo.find(id);

        if (chain == null) {
            throw new DeploymentNotFoundException(id);
        }

        if (chain.getStatus() != ChainRecord.Status.COMPLETED) {
            throw new IllegalStateException(
                "Only completed deployments can be compensated manually."
            );
        }

        rollBack(id);
    }

    public int recoverInterrupted() {
        int recovered = 0;

        for (ChainRecord chain : chainRepo.findAll()) {
            ChainRecord.Status status = chain.getStatus();

            if (status == ChainRecord.Status.STARTED
                || status == ChainRecord.Status.PENDING_B
                || status == ChainRecord.Status.PENDING_C) {
                rollBack(chain.getId());
                recovered++;
            }
        }

        return recovered;
    }
}
