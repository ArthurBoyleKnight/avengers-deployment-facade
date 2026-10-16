import com.starktech.clearance.*;
import com.starktech.diagnostics.*;
import com.starktech.facade.*;
import com.starktech.inventory.*;
import com.starktech.workflow.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentFacadeTest {

    private String failedDeployment(
        Path dir, String drone, String mission, boolean failB, boolean failC
    ) {
        DeploymentFailedException e = assertThrows(
            DeploymentFailedException.class,
            () -> new DeploymentFacade(dir).deploy(drone, mission, failB, failC)
        );
        return e.getChainId();
    }

    private String completedDeployment(Path dir, String drone, String mission) {
        return new DeploymentFacade(dir).deploy(drone, mission, false, false);
    }

    private ChainRecord.Status chainOf(Path dir, String id) {
        return new ChainRepository(dir).find(id).getStatus();
    }

    private DroneUnit.Status droneOf(Path dir, String id) {
        DroneUnit d = new InventoryRepository(dir).find(id);
        return d == null ? null : d.getStatus();
    }

    private DiagnosticReport.Status diagnosticsOf(Path dir, String id) {
        DiagnosticReport r = new DiagnosticRepository(dir).find(id);
        return r == null ? null : r.getStatus();
    }

    private MissionClearance.Status clearanceOf(Path dir, String id) {
        MissionClearance c = new ClearanceRepository(dir).find(id);
        return c == null ? null : c.getStatus();
    }

    private void assertOnlyInventoryWasUndone(Path dir, String id) {
        assertEquals(ChainRecord.Status.COMPENSATED, chainOf(dir, id));
        assertEquals(DroneUnit.Status.RELEASED, droneOf(dir, id));
        assertNull(diagnosticsOf(dir, id));
        assertNull(clearanceOf(dir, id));
    }

    private void assertDiagnosticsThenInventoryWereUndone(Path dir, String id) {
        assertEquals(ChainRecord.Status.COMPENSATED, chainOf(dir, id));
        assertEquals(DiagnosticReport.Status.REVERSED, diagnosticsOf(dir, id));
        assertEquals(DroneUnit.Status.RELEASED, droneOf(dir, id));
        assertNull(clearanceOf(dir, id));
    }

    private void crashedAfterReserving(Path dir, String id, String drone) {
        new ChainRepository(dir).save(new ChainRecord(id, ChainRecord.Status.PENDING_B, "crashed"));
        new InventoryRepository(dir).save(new DroneUnit(id, drone, DroneUnit.Status.RESERVED));
    }

    private void crashedAfterDiagnostics(Path dir, String id, String drone) {
        new ChainRepository(dir).save(new ChainRecord(id, ChainRecord.Status.PENDING_C, "crashed"));
        new InventoryRepository(dir).save(new DroneUnit(id, drone, DroneUnit.Status.RESERVED));
        new DiagnosticRepository(dir).save(new DiagnosticReport(id, drone, DiagnosticReport.Status.PASSED));
    }

    @Test
    void successfulDeploymentPersistsAllThreeSteps(@TempDir Path dir) {
        String id = completedDeployment(dir, "DRONE-01", "M-AVENGER7");

        assertEquals(ChainRecord.Status.COMPLETED, chainOf(dir, id));
        assertEquals(DroneUnit.Status.RESERVED, droneOf(dir, id));
        assertEquals(DiagnosticReport.Status.PASSED, diagnosticsOf(dir, id));
        assertEquals(MissionClearance.Status.APPROVED, clearanceOf(dir, id));
    }

    @Test
    void forcedDiagnosticsFailureUndoesInventoryOnly(@TempDir Path dir) {
        String id = failedDeployment(dir, "DRONE-01", "M-AVENGER7", true, false);

        assertOnlyInventoryWasUndone(dir, id);
    }

    @Test
    void lowBatteryDroneFailsDiagnosticsAndUndoesInventoryOnly(@TempDir Path dir) {
        String id = failedDeployment(dir, "DRONE-01-LOWBAT", "M-AVENGER7", false, false);

        assertOnlyInventoryWasUndone(dir, id);
    }

    @Test
    void forcedClearanceFailureUndoesDiagnosticsThenInventory(@TempDir Path dir) {
        String id = failedDeployment(dir, "DRONE-01", "M-AVENGER7", false, true);

        assertDiagnosticsThenInventoryWereUndone(dir, id);
    }

    @Test
    void deniedMissionFailsClearanceAndUndoesDiagnosticsThenInventory(@TempDir Path dir) {
        String id = failedDeployment(dir, "DRONE-01", "M-AVENGER7-DENIED", false, false);

        assertDiagnosticsThenInventoryWereUndone(dir, id);
    }

    @Test
    void failureCarriesChainIdAndOriginalCause(@TempDir Path dir) {
        DeploymentFailedException e = assertThrows(
            DeploymentFailedException.class,
            () -> new DeploymentFacade(dir).deploy("DRONE-01", "M-AVENGER7", false, true)
        );

        assertTrue(e.getMessage().contains(e.getChainId()));
        assertInstanceOf(ClearanceDeniedException.class, e.getCause());
    }

    @Test
    void alreadyReservedDroneIsRejectedAtFirstStepWithNothingToUndo(@TempDir Path dir) {
        String first = completedDeployment(dir, "DRONE-01", "M-AVENGER7");

        DeploymentFailedException e = assertThrows(
            DeploymentFailedException.class,
            () -> new DeploymentFacade(dir).deploy("DRONE-01", "M-OTHER", false, false)
        );
        String second = e.getChainId();

        assertInstanceOf(DroneUnavailableException.class, e.getCause());
        assertEquals(ChainRecord.Status.REJECTED, chainOf(dir, second));
        assertNull(droneOf(dir, second));
        assertNull(diagnosticsOf(dir, second));
        assertNull(clearanceOf(dir, second));
        assertEquals(ChainRecord.Status.COMPLETED, chainOf(dir, first));
        assertEquals(DroneUnit.Status.RESERVED, droneOf(dir, first));
    }

    @Test
    void droneCanBeDeployedAgainAfterItsDeploymentWasCompensated(@TempDir Path dir) {
        String first = completedDeployment(dir, "DRONE-01", "M-AVENGER7");
        new DeploymentFacade(dir).compensate(first);

        String second = completedDeployment(dir, "DRONE-01", "M-AVENGER7");

        assertEquals(ChainRecord.Status.COMPLETED, chainOf(dir, second));
    }

    @Test
    void invalidInputIsRejectedBeforeAnythingIsSaved(@TempDir Path dir) {
        DeploymentFacade facade = new DeploymentFacade(dir);

        assertThrows(IllegalArgumentException.class, () -> facade.deploy("BAD", "M-AVENGER7", false, false));
        assertThrows(IllegalArgumentException.class, () -> facade.deploy("DRONE-01", "BAD", false, false));
        assertFalse(Files.exists(dir.resolve("chains")));
    }

    @Test
    void completedDeploymentSurvivesRestartAndCanBeCompensated(@TempDir Path dir) {
        String id = completedDeployment(dir, "DRONE-01", "M-AVENGER7");

        DeploymentFacade restarted = new DeploymentFacade(dir);
        assertEquals(ChainRecord.Status.COMPLETED, restarted.inspect(id).getStatus());

        restarted.compensate(id);

        assertEquals(ChainRecord.Status.COMPENSATED, chainOf(dir, id));
        assertEquals(DroneUnit.Status.RELEASED, droneOf(dir, id));
        assertEquals(DiagnosticReport.Status.REVERSED, diagnosticsOf(dir, id));
        assertEquals(MissionClearance.Status.REVOKED, clearanceOf(dir, id));
    }

    @Test
    void interruptedAfterDiagnosticsIsRecoveredByUndoingDiagnosticsThenInventory(@TempDir Path dir) {
        crashedAfterDiagnostics(dir, "crash01", "DRONE-01");

        DeploymentFacade restarted = new DeploymentFacade(dir);
        assertEquals(ChainRecord.Status.PENDING_C, restarted.inspect("crash01").getStatus());

        assertEquals(1, restarted.recoverInterrupted());
        assertDiagnosticsThenInventoryWereUndone(dir, "crash01");
    }

    @Test
    void interruptedAfterReservingIsRecoveredByUndoingInventoryOnly(@TempDir Path dir) {
        crashedAfterReserving(dir, "crash02", "DRONE-02");

        assertEquals(1, new DeploymentFacade(dir).recoverInterrupted());
        assertOnlyInventoryWasUndone(dir, "crash02");
    }

    @Test
    void recoveryLeavesFinishedDeploymentsAlone(@TempDir Path dir) {
        String done = completedDeployment(dir, "DRONE-01", "M-AVENGER7");
        failedDeployment(dir, "DRONE-02", "M-AVENGER7", false, true);

        assertEquals(0, new DeploymentFacade(dir).recoverInterrupted());
        assertEquals(ChainRecord.Status.COMPLETED, chainOf(dir, done));
    }

    @Test
    void adminFacadeInspectsReportsListsAndCompensates(@TempDir Path dir) {
        DeploymentFacade facade = new DeploymentFacade(dir);
        AdminFacade admin = new AdminFacade(facade);
        String id = facade.deploy("DRONE-01", "M-AVENGER7", false, false);

        assertEquals(ChainRecord.Status.COMPLETED, admin.inspect(id).getStatus());
        assertEquals(1, admin.list().size());

        DeploymentReport before = admin.report(id);
        assertEquals("RESERVED", before.inventory());
        assertEquals("PASSED", before.diagnostics());
        assertEquals("APPROVED", before.clearance());

        admin.compensate(id);

        DeploymentReport after = admin.report(id);
        assertEquals("COMPENSATED", after.chain());
        assertEquals("RELEASED", after.inventory());
        assertEquals("REVERSED", after.diagnostics());
        assertEquals("REVOKED", after.clearance());
    }

    @Test
    void adminFacadeRefusesUnknownAndAlreadyCompensatedDeployments(@TempDir Path dir) {
        DeploymentFacade facade = new DeploymentFacade(dir);
        AdminFacade admin = new AdminFacade(facade);
        String id = facade.deploy("DRONE-01", "M-AVENGER7", false, false);
        admin.compensate(id);

        assertThrows(IllegalStateException.class, () -> admin.compensate(id));
        assertThrows(DeploymentNotFoundException.class, () -> admin.compensate("ghost"));
        assertThrows(DeploymentNotFoundException.class, () -> admin.report("ghost"));
        assertNull(admin.inspect("ghost"));
        assertTrue(new AdminFacade(new DeploymentFacade(dir.resolve("empty"))).list().isEmpty());
    }

    @Test
    void inventoryWorksStandaloneAndEnforcesTransitions(@TempDir Path dir) {
        InventoryService service = new InventoryService();
        InventoryRepository repo = new InventoryRepository(dir);

        service.reserve("u1", "DRONE-01", repo);
        assertEquals(DroneUnit.Status.RESERVED, repo.find("u1").getStatus());
        assertThrows(DroneUnavailableException.class, () -> service.reserve("u2", "DRONE-01", repo));

        service.release("u1", repo);
        assertEquals(DroneUnit.Status.RELEASED, repo.find("u1").getStatus());
        assertThrows(IllegalStateException.class, () -> service.release("u1", repo));
        assertThrows(IllegalArgumentException.class, () -> service.validateDroneId("nope"));

        DroneUnit fresh = new DroneUnit("u3", "DRONE-03", DroneUnit.Status.NEW);
        assertThrows(IllegalStateException.class, () -> fresh.transitionTo(DroneUnit.Status.RELEASED));
        repo.delete("u1");
        assertNull(repo.find("u1"));
    }

    @Test
    void diagnosticsWorkStandaloneAndEnforceTransitions(@TempDir Path dir) {
        DiagnosticService service = new DiagnosticService();
        DiagnosticRepository repo = new DiagnosticRepository(dir);

        service.process("d1", "DRONE-01", false, repo);
        assertEquals(DiagnosticReport.Status.PASSED, repo.find("d1").getStatus());
        service.reverse("d1", repo);
        assertEquals(DiagnosticReport.Status.REVERSED, repo.find("d1").getStatus());
        assertThrows(IllegalStateException.class, () -> service.reverse("d1", repo));

        assertThrows(DiagnosticsFailedException.class, () -> service.process("d2", "DRONE-01", true, repo));
        assertThrows(DiagnosticsFailedException.class, () -> service.process("d3", "DRONE-01-LOWBAT", false, repo));
        assertNull(repo.find("d2"));
        assertNull(repo.find("d3"));

        DiagnosticReport fresh = new DiagnosticReport("d4", "DRONE-04", DiagnosticReport.Status.NEW);
        assertThrows(IllegalStateException.class, () -> fresh.transitionTo(DiagnosticReport.Status.REVERSED));
        repo.delete("d1");
        assertNull(repo.find("d1"));
    }

    @Test
    void clearanceWorksStandaloneAndEnforcesTransitions(@TempDir Path dir) {
        ClearanceService service = new ClearanceService();
        ClearanceRepository repo = new ClearanceRepository(dir);

        service.approve("c1", "M-ALPHA", false, repo);
        assertEquals(MissionClearance.Status.APPROVED, repo.find("c1").getStatus());
        service.revoke("c1", repo);
        assertEquals(MissionClearance.Status.REVOKED, repo.find("c1").getStatus());
        assertThrows(IllegalStateException.class, () -> service.revoke("c1", repo));

        assertThrows(ClearanceDeniedException.class, () -> service.approve("c2", "M-ALPHA", true, repo));
        assertThrows(ClearanceDeniedException.class, () -> service.approve("c3", "M-ALPHA-DENIED", false, repo));
        assertNull(repo.find("c2"));
        assertNull(repo.find("c3"));
        assertThrows(IllegalArgumentException.class, () -> service.validateMissionCode("nope"));

        MissionClearance fresh = new MissionClearance("c4", "M-X", MissionClearance.Status.NEW);
        assertThrows(IllegalStateException.class, () -> fresh.transitionTo(MissionClearance.Status.REVOKED));
        repo.delete("c1");
        assertNull(repo.find("c1"));
    }
}
