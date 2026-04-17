package com.xy.bi.model.enums;

public enum ChartStatusEnum {
    WAIT("wait"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),;
    private String status;

    ChartStatusEnum(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
