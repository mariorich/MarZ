package com.marz.scheduler.service;

import com.marz.scheduler.entity.JobDefinition;
import com.marz.scheduler.repository.JobDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marz.scheduler.dto.CreateJobDefinitionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobDefinitionService {

    private final JobDefinitionRepository repository;

    public JobDefinitionService(JobDefinitionRepository repository) {
        this.repository = repository;
    }

    public List<JobDefinition> findAll() {
        return repository.findAll();
    }

    public List<JobDefinition> findEnabled() {
        return repository.findByEnabledTrueOrderByNameAsc();
    }

    public JobDefinition findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new JobDefinitionNotFoundException(id));
    }

    @Transactional
    public JobDefinition create(CreateJobDefinitionRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A job with this name already exists"
            );
        }

        JobDefinition job = new JobDefinition();
        job.setName(request.getName());
        job.setDescription(request.getDescription());
        job.setJclDataset(request.getJclDataset());
        job.setJclMember(request.getJclMember());
        job.setMaxAttempts(request.getMaxAttempts());

        return repository.save(job);
    }
}
