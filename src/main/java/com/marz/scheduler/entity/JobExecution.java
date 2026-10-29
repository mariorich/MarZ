
package com.marz.scheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
public class JobExecution {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_definition_id", nullable = false)
    private UUID jobDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ExecutionTriggerType triggerType;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    public JobExecution() {
        // Required by JPA.
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = ExecutionStatus.QUEUED;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobDefinitionId() {
        return jobDefinitionId;
    }

    public void setJobDefinitionId(UUID jobDefinitionId) {
        this.jobDefinitionId = jobDefinitionId;
    }

    public ExecutionTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(ExecutionTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(Instant scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }
}
