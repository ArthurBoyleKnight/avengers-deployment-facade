package com.starktech.clearance;

public class MissionClearance {
    public enum Status {
        NEW, APPROVED, REVOKED
    }

    private final String id;
    private final String missionCode;
    private Status status;

    public MissionClearance(String id, String missionCode, Status status) {
        this.id = id;
        this.missionCode = missionCode;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getMissionCode() {
        return missionCode;
    }

    public Status getStatus() {
        return status;
    }

    public void transitionTo(Status next) {
        boolean legal =
            (status == Status.NEW && next == Status.APPROVED)
            || (status == Status.APPROVED && next == Status.REVOKED);

        if (!legal) {
            throw new IllegalStateException(
                "Clearance cannot move from " + status + " to " + next + "."
            );
        }

        status = next;
    }
}
