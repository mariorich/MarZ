package com.marz.scheduler.dto;

import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.ExecutionTriggerType;
import com.marz.scheduler.entity.JobExecution;

import java.time.Instant;
import java.util.UUID;

public record JobExecutionSummaryResponse(
        UUID id,
        UUID jobDefinitionId,
        ExecutionTriggerType triggerType,
        Instant scheduledFor,
        ExecutionStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        UUID scheduleId
) {
    public static JobExecutionSummaryResponse from(JobExecution execution) {
        return new JobExecutionSummaryResponse(
                execution.getId(),
                execution.getJobDefinitionId(),
                execution.getTriggerType(),
                execution.getScheduledFor(),
                execution.getStatus(),
                execution.getCreatedAt(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getErrorMessage(),
                execution.getScheduleId()
        );
    }
}