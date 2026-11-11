package com.marz.scheduler.repository;
import com.marz.scheduler.entity.AttemptStatus;

import com.marz.scheduler.entity.JobAttempt;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;
import java.util.Collection;

public interface JobAttemptRepository
        extends JpaRepository<JobAttempt, UUID> {

    List<JobAttempt> findByExecutionIdOrderByAttemptNumberAsc(
            UUID executionId);
    
    List<JobAttempt> findByStatusInAndJesJobIdIsNotNullOrderBySubmittedAtAsc(
        Collection<AttemptStatus> statuses);


}
