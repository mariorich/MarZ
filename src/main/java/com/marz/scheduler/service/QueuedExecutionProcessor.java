package com.marz.scheduler.service;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.repository.JobAttemptRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueuedExecutionProcessor {

    private static final int BATCH_SIZE = 10;

    private final JobExecutionRepository executionRepository;
    private final JobAttemptRepository attemptRepository;

    public QueuedExecutionProcessor(
            JobExecutionRepository executionRepository,
            JobAttemptRepository attemptRepository) {
        this.executionRepository = executionRepository;
        this.attemptRepository = attemptRepository;
    }

    @Scheduled(
        fixedDelayString = "${marz.worker.poll-interval-ms:2000}"
    )
    @Transactional
    public void processQueuedExecutions() {
        var executions =
                executionRepository.findQueuedForUpdate(BATCH_SIZE);

        for (JobExecution execution : executions) {
            JobAttempt attempt = new JobAttempt();
            attempt.setExecutionId(execution.getId());
            attempt.setAttemptNumber(1);
            attempt.setStatus(AttemptStatus.PENDING);

            attemptRepository.save(attempt);

            execution.setStatus(ExecutionStatus.SUBMITTING);
            executionRepository.save(execution);
        }
    }
}
