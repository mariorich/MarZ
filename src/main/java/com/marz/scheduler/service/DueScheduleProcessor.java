package com.marz.scheduler.service;

import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.ExecutionTriggerType;
import com.marz.scheduler.entity.JobExecution;
import com.marz.scheduler.entity.JobSchedule;
import com.marz.scheduler.repository.JobExecutionRepository;
import com.marz.scheduler.repository.JobScheduleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DueScheduleProcessor {

    private static final int BATCH_SIZE = 25;

    private final JobScheduleRepository scheduleRepository;
    private final JobExecutionRepository executionRepository;

    public DueScheduleProcessor(
            JobScheduleRepository scheduleRepository,
            JobExecutionRepository executionRepository) {
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
    }

    @Scheduled(fixedDelayString =
            "${marz.scheduler.poll-interval-ms:5000}")
    @Transactional
    public void processDueSchedules() {
        Instant now = Instant.now();

        List<JobSchedule> dueSchedules =
                scheduleRepository.findDueSchedulesForUpdate(
                        now, BATCH_SIZE);

        for (JobSchedule schedule : dueSchedules) {
            queueExecution(schedule, now);
        }
    }

    private void queueExecution(
            JobSchedule schedule, Instant now) {

        // Preserve the exact occurrence that became due.
        Instant scheduledFor = schedule.getNextRunAt();

        JobExecution execution = new JobExecution();
        execution.setJobDefinitionId(
                schedule.getJobDefinitionId());
        execution.setScheduleId(schedule.getId());
        execution.setTriggerType(
                ExecutionTriggerType.SCHEDULED);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setScheduledFor(scheduledFor);

        executionRepository.save(execution);

        // Skip missed occurrences rather than generating a
        // backlog after MarZ has been offline.
        ZoneId zone = ZoneId.of(schedule.getTimezone());
        CronExpression cron =
                CronExpression.parse(schedule.getCronExpression());

        ZonedDateTime next = cron.next(
                ZonedDateTime.ofInstant(now, zone));

        if (next == null) {
            throw new IllegalStateException(
                    "No next execution for schedule "
                            + schedule.getId());
        }

        schedule.setNextRunAt(next.toInstant());
        scheduleRepository.save(schedule);
    }
}
