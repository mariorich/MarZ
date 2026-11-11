package com.marz.scheduler.service;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.repository.JobAttemptRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class JobStatusUpdateService {

    private static final Pattern SUCCESS_RETURN_CODE =
            Pattern.compile("CC\\s+0000");

    private final JobAttemptRepository attemptRepository;
    private final JobExecutionRepository executionRepository;

    public JobStatusUpdateService(
            JobAttemptRepository attemptRepository,
            JobExecutionRepository executionRepository) {

        this.attemptRepository = attemptRepository;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public void updateStatus(UUID attemptId, JsonNode zosmfStatus) {

        JobAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job attempt not found: " + attemptId));

        JobExecution execution = executionRepository
                .findById(attempt.getExecutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job execution not found: "
                                + attempt.getExecutionId()));

        // Do not allow a completed attempt to return to a previous state.
        if (attempt.getStatus() == AttemptStatus.SUCCEEDED
                || attempt.getStatus() == AttemptStatus.FAILED) {
            return;
        }

        String zosmfJobStatus = zosmfStatus
                .path("status")
                .asText("")
                .trim()
                .toUpperCase(Locale.ROOT);

        String returnCode = zosmfStatus
                .path("retcode")
                .asText("")
                .trim();

        if ("ACTIVE".equals(zosmfJobStatus)) {

            Instant now = Instant.now();

            attempt.setStatus(AttemptStatus.RUNNING);

            if (attempt.getStartedAt() == null) {
                attempt.setStartedAt(now);
            }

            execution.setStatus(ExecutionStatus.RUNNING);

            if (execution.getStartedAt() == null) {
                execution.setStartedAt(now);
            }

        } else if ("OUTPUT".equals(zosmfJobStatus)) {

            Instant now = Instant.now();

            attempt.setReturnCode(
                    returnCode.isBlank() ? null : returnCode);

            attempt.setFinishedAt(now);
            execution.setFinishedAt(now);

            if (SUCCESS_RETURN_CODE.matcher(returnCode).matches()) {

                attempt.setStatus(AttemptStatus.SUCCEEDED);
                execution.setStatus(ExecutionStatus.SUCCEEDED);

            } else {

                attempt.setStatus(AttemptStatus.FAILED);
                execution.setStatus(ExecutionStatus.FAILED);

                if (returnCode.isBlank()) {
                    execution.setErrorMessage(
                            "JES reported OUTPUT without a return code.");
                } else {
                    execution.setErrorMessage(
                            "JES job failed with return code: "
                                    + returnCode);
                }
            }
        }

        // Other JES statuses are deliberately left unchanged.
    }
}
