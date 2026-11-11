package com.marz.scheduler.dto;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.JobAttempt;

import java.time.Instant;
import java.util.UUID;

public record JobAttemptResponse(
        UUID id,
        int attemptNumber,
        String jesJobName,
        String jesJobId,
        AttemptStatus status,
        String returnCode,
        Instant submittedAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static JobAttemptResponse from(JobAttempt attempt) {
        return new JobAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getJesJobName(),
                attempt.getJesJobId(),
                attempt.getStatus(),
                attempt.getReturnCode(),
                attempt.getSubmittedAt(),
                attempt.getStartedAt(),
                attempt.getFinishedAt()
        );
    }
}