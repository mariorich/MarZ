package com.marz.scheduler.controller;

import com.marz.scheduler.service.ZosmfJobStatusService;
import tools.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zosmf")
public class ZosmfJobStatusDiagnosticController {

    private final ZosmfJobStatusService jobStatusService;

    public ZosmfJobStatusDiagnosticController(
            ZosmfJobStatusService jobStatusService) {
        this.jobStatusService = jobStatusService;
    }

    @GetMapping("/jobs/{jobName}/{jobId}/status")
    public JsonNode getJobStatus(
            @PathVariable String jobName,
            @PathVariable String jobId) {

        return jobStatusService.getJobStatus(jobName, jobId);
    }
}
