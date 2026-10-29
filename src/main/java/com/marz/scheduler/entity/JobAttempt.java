package com.marz.scheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "job_attempts",
    uniqueConstraints = @UniqueConstraint(
        name = "job_attempts_execution_id_attempt_number_key",
        columnNames = {"execution_id", "attempt_number"}
    )
)
public class JobAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Min(1)
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "jes_job_id")
    private String jesJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status = AttemptStatus.PENDING;

    @Column(name = "return_code")
    private Integer returnCode;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public JobAttempt() {
        // Required by JPA.
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = AttemptStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getJesJobId() {
        return jesJobId;
    }

    public void setJesJobId(String jesJobId) {
        this.jesJobId = jesJobId;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
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
}
