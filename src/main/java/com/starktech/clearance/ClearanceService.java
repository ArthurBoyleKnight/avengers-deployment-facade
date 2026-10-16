package com.starktech.clearance;

public class ClearanceService {

    public void validateMissionCode(String missionCode) {
        if (missionCode == null
            || !missionCode.matches("M-[A-Za-z0-9]+(-[A-Za-z0-9]+)*")) {
            throw new IllegalArgumentException("Invalid mission code.");
        }
    }

    public void approve(
        String id,
        String missionCode,
        boolean fail,
        ClearanceRepository repository
    ) {
        if (fail) {
            throw new ClearanceDeniedException("Mission clearance failed.");
        }

        if (missionCode.endsWith("-DENIED")) {
            throw new ClearanceDeniedException(
                "Mission clearance failed: mission is on the denied list."
            );
        }

        MissionClearance clearance = new MissionClearance(
            id,
            missionCode,
            MissionClearance.Status.NEW
        );
        clearance.transitionTo(MissionClearance.Status.APPROVED);

        repository.save(clearance);
    }

    public void revoke(String id, ClearanceRepository repository) {
        MissionClearance clearance = repository.find(id);

        if (clearance != null) {
            clearance.transitionTo(MissionClearance.Status.REVOKED);
            repository.save(clearance);
        }
    }
}
