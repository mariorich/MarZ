package com.marz.scheduler.repository;

import com.marz.scheduler.entity.JobAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobAttemptRepository
        extends JpaRepository<JobAttempt, UUID> {

    List<JobAttempt> findByExecutionIdOrderByAttemptNumberAsc(
            UUID executionId);
}
