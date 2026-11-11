package com.marz.scheduler.service;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.repository.JobAttemptRepository;
import com.marz.scheduler.repository.JobDefinitionRepository;
import com.marz.scheduler.repository.JobExecutionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Service
public class QueuedExecutionProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(QueuedExecutionProcessor.class);

    private final QueuedExecutionClaimService claimService;
    private final JobDefinitionRepository definitionRepository;
    private final JobAttemptRepository attemptRepository;
    private final JobExecutionRepository executionRepository;
    private final ZosmfJobSubmissionService submissionService;

    public QueuedExecutionProcessor(
            QueuedExecutionClaimService claimService,
            JobDefinitionRepository definitionRepository,
            JobAttemptRepository attemptRepository,
            JobExecutionRepository executionRepository,
            ZosmfJobSubmissionService submissionService) {

        this.claimService = claimService;
        this.definitionRepository = definitionRepository;
        this.attemptRepository = attemptRepository;
        this.executionRepository = executionRepository;
        this.submissionService = submissionService;
    }

    @Scheduled(
        fixedDelayString = "${marz.worker.poll-interval-ms:2000}"
    )
    public void processQueuedExecutions() {

        var claimedExecutions =
                claimService.claimQueuedExecutions();

        for (var claimed : claimedExecutions) {

            try {
                // Load the job definition.
                var definition = definitionRepository
                        .findById(claimed.jobDefinitionId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Job definition not found: "
                                        + claimed.jobDefinitionId()));

                log.info("Submitting job definition: {}",
                        definition.getName());

                // Submit JCL through z/OSMF.
                JsonNode response =
                        submissionService.submitDatasetMember(
                                definition.getJclDataset(),
                                definition.getJclMember()
                        );

                // Extract JES identifiers.
                String jobName = response.path("jobname").asText();
                String jobId = response.path("jobid").asText();

                if (jobName.isBlank() || jobId.isBlank()) {
                    throw new IllegalStateException(
                            "z/OSMF response is missing jobname or jobid.");
                }

                // Update the attempt.
                JobAttempt attempt = attemptRepository
                        .findById(claimed.attemptId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Job attempt not found: "
                                        + claimed.attemptId()));

                attempt.setJesJobName(jobName);
                attempt.setJesJobId(jobId);
                attempt.setStatus(AttemptStatus.SUBMITTED);
                attempt.setSubmittedAt(Instant.now());

                attemptRepository.save(attempt);

                log.info(
                        "Job submitted successfully. JES job: {} / {}",
                        jobName,
                        jobId
                );

            } catch (Exception e) {

                log.error(
                        "Could not confirm submission for execution {}. "
                                + "Manual reconciliation may be required.",
                        claimed.executionId(),
                        e
                );

                // Mark the attempt as UNKNOWN.
                try {
                    JobAttempt attempt = attemptRepository
                            .findById(claimed.attemptId())
                            .orElseThrow();

                    attempt.setStatus(AttemptStatus.UNKNOWN);
                    attemptRepository.save(attempt);

                } catch (Exception persistenceException) {
                    log.error(
                            "Could not update attempt {} to UNKNOWN.",
                            claimed.attemptId(),
                            persistenceException
                    );
                }
            }
        }
    }
}
