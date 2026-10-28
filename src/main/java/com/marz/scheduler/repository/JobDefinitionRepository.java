package com.marz.scheduler.repository;

import com.marz.scheduler.entity.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobDefinitionRepository
        extends JpaRepository<JobDefinition, UUID> {

    Optional<JobDefinition> findByName(String name);
    
    boolean existsByName(String name);

    List<JobDefinition> findByEnabledTrueOrderByNameAsc();

}
