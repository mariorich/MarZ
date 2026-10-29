package com.marz.scheduler.controller;

import com.marz.scheduler.dto.CreateJobScheduleRequest;
import com.marz.scheduler.entity.JobSchedule;
import com.marz.scheduler.service.JobScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
public class JobScheduleController {

    private final JobScheduleService service;

    public JobScheduleController(JobScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobSchedule> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public JobSchedule findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/job/{jobId}")
    public List<JobSchedule> findByJob(
            @PathVariable UUID jobId) {
        return service.findByJobDefinitionId(jobId);
    }

    @PostMapping
    public ResponseEntity<JobSchedule> create(
            @Valid @RequestBody CreateJobScheduleRequest request) {

        JobSchedule created = service.create(request);

        return ResponseEntity
                .created(URI.create("/api/schedules/" + created.getId()))
                .body(created);
    }
}
