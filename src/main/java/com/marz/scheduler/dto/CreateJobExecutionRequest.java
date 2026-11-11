package com.marz.scheduler.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateJobExecutionRequest(
        @NotNull UUID jobDefinitionId
) {
}
