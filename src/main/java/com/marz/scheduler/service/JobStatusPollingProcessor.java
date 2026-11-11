package com.marz.scheduler.service;

import com.marz.scheduler.entity.AttemptStatus;
import com.marz.scheduler.entity.JobAttempt;
import com.marz.scheduler.repository.JobAttemptRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
public class JobStatusPollingProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(JobStatusPollingProcessor.class);

    private final JobAttemptRepository attemptRepository;
    private final ZosmfJobStatusService jobStatusService;
    private final JobStatusUpdateService statusUpdateService;

    public JobStatusPollingProcessor(
            JobAttemptRepository attemptRepository,
            ZosmfJobStatusService jobStatusService,
            JobStatusUpdateService statusUpdateService) {

        this.attemptRepository = attemptRepository;
        this.jobStatusService = jobStatusService;
        this.statusUpdateService = statusUpdateService;
    }

    @Scheduled(
        fixedDelayString = "${marz.monitor.poll-interval-ms:5000}"
    )
    public void pollSubmittedJobs() {

        List<JobAttempt> attempts = attemptRepository
                .findByStatusInAndJesJobIdIsNotNullOrderBySubmittedAtAsc(
                        List.of(
                                AttemptStatus.SUBMITTED,
                                AttemptStatus.RUNNING
                        )
                );

        for (JobAttempt attempt : attempts) {
            pollJob(attempt);
        }
    }

    private void pollJob(JobAttempt attempt) {

        String jobName = attempt.getJesJobName();
        String jobId = attempt.getJesJobId();

        try {
            log.debug(
                    "Polling JES job status: {} / {}",
                    jobName,
                    jobId
            );

            JsonNode zosmfStatus =
                    jobStatusService.getJobStatus(jobName, jobId);

            statusUpdateService.updateStatus(
                    attempt.getId(),
                    zosmfStatus
            );

            log.debug(
                    "JES job {} / {} status: {}",
                    jobName,
                    jobId,
                    zosmfStatus.path("status").asText()
            );

        } catch (Exception e) {

            log.warn(
                    "Could not retrieve or persist status for JES job "
                            + "{} / {}. It will be retried on the next "
                            + "polling cycle.",
                    jobName,
                    jobId,
                    e
            );
        }
    }
}
