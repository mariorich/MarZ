package com.marz.scheduler.service;

import com.marz.scheduler.dto.CreateJobScheduleRequest;
import com.marz.scheduler.entity.JobSchedule;
import com.marz.scheduler.repository.JobDefinitionRepository;
import com.marz.scheduler.repository.JobScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobScheduleService {

    private final JobScheduleRepository scheduleRepository;
    private final JobDefinitionRepository jobRepository;

    public JobScheduleService(
            JobScheduleRepository scheduleRepository,
            JobDefinitionRepository jobRepository) {
        this.scheduleRepository = scheduleRepository;
        this.jobRepository = jobRepository;
    }

    public List<JobSchedule> findAll() {
        return scheduleRepository.findAll();
    }

    public JobSchedule findById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Schedule not found: " + id));
    }

    public List<JobSchedule> findByJobDefinitionId(UUID jobId) {
        return scheduleRepository
                .findByJobDefinitionIdOrderByCreatedAtDesc(jobId);
    }

    @Transactional
    public JobSchedule create(CreateJobScheduleRequest request) {

        if (!jobRepository.existsById(request.getJobDefinitionId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job definition not found: "
                            + request.getJobDefinitionId());
        }

        final ZoneId zone;

        try {
            zone = ZoneId.of(request.getTimezone());
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid timezone: " + request.getTimezone());
        }

        final CronExpression cron;

        try {
            cron = CronExpression.parse(request.getCronExpression());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid cron expression: "
                            + request.getCronExpression());
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = cron.next(now);

        if (next == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cron expression has no next execution time");
        }

        JobSchedule schedule = new JobSchedule();
        schedule.setJobDefinitionId(request.getJobDefinitionId());
        schedule.setCronExpression(request.getCronExpression());
        schedule.setTimezone(zone.getId());
        schedule.setEnabled(true);
        schedule.setNextRunAt(next.toInstant());
        schedule.setLastRunAt(null);

        return scheduleRepository.save(schedule);
    }
}
