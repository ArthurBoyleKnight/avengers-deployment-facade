package com.starktech.workflow;

public class ChainRecord {

    public enum Status {
        STARTED,
        PENDING_B,
        PENDING_C,
        COMPLETED,
        COMPENSATED,
        REJECTED
    }

    private final String id;
    private Status status;
    private String detail;

    public ChainRecord(String id, Status status, String detail) {
        this.id = id;
        this.status = status;
        this.detail = detail;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
