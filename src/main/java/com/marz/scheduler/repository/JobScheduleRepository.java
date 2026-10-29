package com.marz.scheduler.repository;

import com.marz.scheduler.entity.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobScheduleRepository
        extends JpaRepository<JobSchedule, UUID> {

    List<JobSchedule> findByJobDefinitionId(UUID jobDefinitionId);

    List<JobSchedule> findByEnabledTrue();

    List<JobSchedule> findByJobDefinitionIdOrderByCreatedAtDesc(
            UUID jobDefinitionId);
}
