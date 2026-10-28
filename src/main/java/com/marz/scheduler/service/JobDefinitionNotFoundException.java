package com.marz.scheduler.service;

import java.util.UUID;

public class JobDefinitionNotFoundException
        extends RuntimeException {

    public JobDefinitionNotFoundException(UUID id) {
        super("Job definition not found: " + id);
    }
}
