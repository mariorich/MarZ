package com.marz.scheduler.controller;

import com.marz.scheduler.dto.CreateJobExecutionRequest;
import com.marz.scheduler.dto.JobExecutionDetailResponse;
import com.marz.scheduler.dto.JobExecutionSummaryResponse;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.service.JobExecutionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class JobExecutionController {

    private final JobExecutionService service;

    public JobExecutionController(JobExecutionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobExecution> create(
            @Valid @RequestBody CreateJobExecutionRequest request) {

        JobExecution created =
                service.createManualExecution(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/executions/" + created.getId()
                ))
                .body(created);
    }

    @PostMapping("/{executionId}/retry")
    public ResponseEntity<JobExecution> retry(
            @PathVariable UUID executionId) {

        return ResponseEntity.ok(
                service.retryExecution(executionId)
        );
    }

    @GetMapping
    public ResponseEntity<List<JobExecutionSummaryResponse>> getAll() {
        return ResponseEntity.ok(service.getAllExecutions());
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<JobExecutionDetailResponse> getById(
            @PathVariable UUID executionId) {

        return ResponseEntity.ok(
                service.getExecution(executionId)
        );
    }

    @GetMapping("/job-definition/{jobDefinitionId}")
    public ResponseEntity<List<JobExecutionSummaryResponse>> getByJobDefinition(
            @PathVariable UUID jobDefinitionId) {

        return ResponseEntity.ok(
                service.getExecutionsForJob(jobDefinitionId)
        );
    }
}
