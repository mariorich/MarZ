package com.marz.scheduler.entity;

public enum ExecutionStatus {
    QUEUED,
    WAITING_DEPENDENCY,
    SUBMITTING,
    RUNNING,
    RETRY_WAIT,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    SKIPPED
}
