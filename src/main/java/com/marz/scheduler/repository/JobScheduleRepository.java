package com.marz.scheduler.repository;

import com.marz.scheduler.entity.JobSchedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface JobScheduleRepository
        extends JpaRepository<JobSchedule, UUID> {

    List<JobSchedule> findByJobDefinitionId(UUID jobDefinitionId);

    List<JobSchedule> findByEnabledTrue();

    List<JobSchedule> findByJobDefinitionIdOrderByCreatedAtDesc(
            UUID jobDefinitionId);

    @Query(value = """
        SELECT s.*
        FROM job_schedules s
        JOIN job_definitions j
        ON j.id = s.job_definition_id
        WHERE s.enabled = true
        AND j.enabled = true
        AND s.next_run_at IS NOT NULL
        AND s.next_run_at <= :now
        ORDER BY s.next_run_at ASC
        LIMIT :batchSize
        FOR UPDATE OF s SKIP LOCKED
        """, nativeQuery = true)
        List<JobSchedule> findDueSchedulesForUpdate(
                @Param("now") Instant now,
                @Param("batchSize") int batchSize);
}
