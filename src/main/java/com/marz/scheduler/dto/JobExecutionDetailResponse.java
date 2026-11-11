package com.marz.scheduler.dto;

import java.util.List;

public record JobExecutionDetailResponse(
        JobExecutionSummaryResponse execution,
        List<JobAttemptResponse> attempts
) {
}