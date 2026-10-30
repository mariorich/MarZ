package com.marz.scheduler.controller;

import com.marz.scheduler.dto.ZosmfSubmitRequest;
import com.marz.scheduler.dto.ZosmfSubmitResponse;
import com.marz.scheduler.service.ZosmfJobSubmissionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/zosmf/jobs")
public class ZosmfJobController {

    private final ZosmfJobSubmissionService submissionService;

    public ZosmfJobController(
            ZosmfJobSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<ZosmfSubmitResponse> submitJob(
            @Valid @RequestBody ZosmfSubmitRequest request) {

        JsonNode result = submissionService.submitDatasetMember(
                request.dataset(),
                request.member()
        );

        ZosmfSubmitResponse response = new ZosmfSubmitResponse(
                result.path("jobid").asText(),
                result.path("jobname").asText()
        );

        return ResponseEntity.accepted().body(response);
    }
}