package com.marz.scheduler.controller;

import java.util.Map;

import com.marz.scheduler.service.ZosmfJobSubmissionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zosmf")
public class ZosmfDiagnosticController {

    private final ZosmfJobSubmissionService submissionService;

    public ZosmfDiagnosticController(
            ZosmfJobSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        int status = submissionService.testConnection();

        return ResponseEntity.ok(Map.of(
                "connected", true,
                "httpStatus", status,
                "message", "Successfully connected to z/OSMF."
        ));
    }
}
