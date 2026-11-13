package com.marz.scheduler.service;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.repository.JobAttemptRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class QueuedExecutionClaimService {

    private static final int BATCH_SIZE = 10;

    private final JobExecutionRepository executionRepository;
    private final JobAttemptRepository attemptRepository;

    public QueuedExecutionClaimService(
            JobExecutionRepository executionRepository,
            JobAttemptRepository attemptRepository) {
        this.executionRepository = executionRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public List<ClaimedExecution> claimQueuedExecutions() {

        var executions =
                executionRepository.findQueuedForUpdate(BATCH_SIZE);

        return executions.stream()
                .map(this::claimExecution)
                .toList();
    }

    private ClaimedExecution claimExecution(JobExecution execution) {

        List<JobAttempt> previousAttempts = attemptRepository
                .findByExecutionIdOrderByAttemptNumberAsc(execution.getId());

        int nextAttemptNumber = previousAttempts.stream()
                .mapToInt(JobAttempt::getAttemptNumber)
                .max()
                .orElse(0) + 1;

        JobAttempt attempt = new JobAttempt();
        attempt.setExecutionId(execution.getId());
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setStatus(AttemptStatus.PENDING);

        attemptRepository.save(attempt);

        execution.setStatus(ExecutionStatus.SUBMITTING);
        executionRepository.save(execution);

        return new ClaimedExecution(
                execution.getId(),
                execution.getJobDefinitionId(),
                attempt.getId()
        );
    }

    public record ClaimedExecution(
            UUID executionId,
            UUID jobDefinitionId,
            UUID attemptId
    ) {}
}
