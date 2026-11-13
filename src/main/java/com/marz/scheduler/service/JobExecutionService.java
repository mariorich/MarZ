package com.marz.scheduler.service;

import com.marz.scheduler.dto.CreateJobExecutionRequest;
import com.marz.scheduler.dto.JobAttemptResponse;
import com.marz.scheduler.dto.JobExecutionDetailResponse;
import com.marz.scheduler.dto.JobExecutionSummaryResponse;
import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.ExecutionTriggerType;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.repository.JobAttemptRepository;
import com.marz.scheduler.repository.JobDefinitionRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobExecutionService {

    private final JobDefinitionRepository definitionRepository;
    private final JobExecutionRepository executionRepository;
    private final JobAttemptRepository attemptRepository;

    public JobExecutionService(
            JobDefinitionRepository definitionRepository,
            JobExecutionRepository executionRepository,
            JobAttemptRepository attemptRepository) {

        this.definitionRepository = definitionRepository;
        this.executionRepository = executionRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public JobExecution createManualExecution(
            CreateJobExecutionRequest request) {

        var definition = definitionRepository
                .findById(request.jobDefinitionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job definition not found: "
                                + request.jobDefinitionId()
                ));

        if (!definition.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Job definition is disabled"
            );
        }

        JobExecution execution = new JobExecution();

        execution.setJobDefinitionId(definition.getId());
        execution.setTriggerType(ExecutionTriggerType.MANUAL);
        execution.setStatus(ExecutionStatus.QUEUED);

        return executionRepository.save(execution);
    }

    @Transactional
    public JobExecution retryExecution(UUID executionId) {

        JobExecution execution = executionRepository
                .findByIdForUpdate(executionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Execution not found: " + executionId
                ));

        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only FAILED executions can be retried"
            );
        }

        List<JobAttempt> attempts = attemptRepository
                .findByExecutionIdOrderByAttemptNumberAsc(executionId);

        if (attempts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Execution has no attempts to retry"
            );
        }

        if (attempts.stream().anyMatch(
                attempt -> attempt.getStatus() == AttemptStatus.UNKNOWN)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Execution has an UNKNOWN attempt. Reconcile it before retrying."
            );
        }

        JobAttempt latestAttempt = attempts.get(attempts.size() - 1);

        if (latestAttempt.getStatus() != AttemptStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The latest attempt must be FAILED before it can be retried"
            );
        }

        var definition = definitionRepository
                .findById(execution.getJobDefinitionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job definition not found: "
                                + execution.getJobDefinitionId()
                ));

        int nextAttemptNumber = latestAttempt.getAttemptNumber() + 1;
        int maxAttempts = definition.getMaxAttempts();

        // Zero means unlimited. Otherwise maxAttempts includes the first run.
        if (maxAttempts > 0 && nextAttemptNumber > maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Maximum attempts reached (" + maxAttempts + ")"
            );
        }

        // The claim worker creates the next PENDING attempt when it claims
        // this queued execution. Existing attempts remain unchanged.
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setStartedAt(null);
        execution.setFinishedAt(null);
        execution.setErrorMessage(null);

        return executionRepository.save(execution);
    }

    public List<JobExecutionSummaryResponse> getAllExecutions() {
        return executionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(JobExecutionSummaryResponse::from)
                .toList();
    }

    public JobExecutionDetailResponse getExecution(UUID executionId) {
        JobExecution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Execution not found: " + executionId
                ));

        List<JobAttemptResponse> attempts = attemptRepository
                .findByExecutionIdOrderByAttemptNumberAsc(executionId)
                .stream()
                .map(JobAttemptResponse::from)
                .toList();

        return new JobExecutionDetailResponse(
                JobExecutionSummaryResponse.from(execution),
                attempts
        );
    }

    public List<JobExecutionSummaryResponse> getExecutionsForJob(
            UUID jobDefinitionId) {

        return executionRepository
                .findByJobDefinitionIdOrderByCreatedAtDesc(jobDefinitionId)
                .stream()
                .map(JobExecutionSummaryResponse::from)
                .toList();
    }
}
