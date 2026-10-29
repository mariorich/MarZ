package com.marz.scheduler.repository;

import com.marz.scheduler.entity.ExecutionStatus;
import com.marz.scheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository
        extends JpaRepository<JobExecution, UUID> {

    List<JobExecution> findByJobDefinitionIdOrderByCreatedAtDesc(
            UUID jobDefinitionId);

    List<JobExecution> findByStatusOrderByCreatedAtAsc(
            ExecutionStatus status);

        @Query(value = """
        SELECT e.*
        FROM job_executions e
        WHERE e.status = 'QUEUED'
        ORDER BY e.created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
        List<JobExecution> findQueuedForUpdate(
                @Param("batchSize") int batchSize);
}
