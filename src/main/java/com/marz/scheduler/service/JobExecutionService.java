package com.marz.scheduler.service;

import com.marz.scheduler.dto.CreateJobExecutionRequest;
import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.ExecutionTriggerType;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.repository.JobDefinitionRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class JobExecutionService {

    private final JobDefinitionRepository definitionRepository;
    private final JobExecutionRepository executionRepository;

    public JobExecutionService(
            JobDefinitionRepository definitionRepository,
            JobExecutionRepository executionRepository) {

        this.definitionRepository = definitionRepository;
        this.executionRepository = executionRepository;
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
}
