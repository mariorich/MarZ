package com.marz.scheduler.controller;

import com.marz.scheduler.entity.JobDefinition;
import com.marz.scheduler.service.JobDefinitionService;
import com.marz.scheduler.dto.CreateJobDefinitionRequest;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;

import java.net.URI;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobDefinitionController {

    private final JobDefinitionService service;

    public JobDefinitionController(JobDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobDefinition> findAll() {
        return service.findAll();
    }

    @GetMapping("/enabled")
    public List<JobDefinition> findEnabled() {
        return service.findEnabled();
    }

    @GetMapping("/{id}")
    public JobDefinition findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<JobDefinition> create(
            @Valid @RequestBody CreateJobDefinitionRequest request) {

        JobDefinition created = service.create(request);

        return ResponseEntity
                .created(URI.create("/api/jobs/" + created.getId()))
                .body(created);
    }
}
