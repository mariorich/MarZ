package com.marz.scheduler.dto;

import jakarta.validation.constraints.NotBlank;

public record ZosmfSubmitRequest(
        @NotBlank String dataset,
        @NotBlank String member
) {
}