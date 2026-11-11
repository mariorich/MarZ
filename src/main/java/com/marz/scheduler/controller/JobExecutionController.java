package com.marz.scheduler.controller;

import com.marz.scheduler.dto.CreateJobExecutionRequest;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.service.JobExecutionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
}
