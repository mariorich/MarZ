-- V1__create_job_tables.sql
-- MarZ: initial schema for job definitions, schedules, dependencies,
-- logical executions, JES submission attempts, and execution events.

CREATE TABLE job_definitions (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    jcl_dataset     VARCHAR(44) NOT NULL,
    jcl_member      VARCHAR(8) NOT NULL,

    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    max_attempts    INTEGER NOT NULL DEFAULT 1
                    CHECK (max_attempts >= 1),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE job_schedules (
    id                  UUID PRIMARY KEY,
    job_definition_id   UUID NOT NULL
        REFERENCES job_definitions(id) ON DELETE CASCADE,

    cron_expression     VARCHAR(100) NOT NULL,
    timezone            VARCHAR(60) NOT NULL DEFAULT 'UTC',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,

    next_run_at         TIMESTAMPTZ,
    last_run_at         TIMESTAMPTZ,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_schedules_due
    ON job_schedules (next_run_at)
    WHERE enabled = TRUE;

CREATE TABLE job_dependencies (
    id                  UUID PRIMARY KEY,
    job_definition_id   UUID NOT NULL
        REFERENCES job_definitions(id) ON DELETE CASCADE,

    depends_on_job_id   UUID NOT NULL
        REFERENCES job_definitions(id) ON DELETE CASCADE,

    required_status     VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED'
        CHECK (required_status IN ('SUCCEEDED')),

    UNIQUE (job_definition_id, depends_on_job_id),
    CHECK (job_definition_id <> depends_on_job_id)
);

CREATE TABLE job_executions (
    id                  UUID PRIMARY KEY,
    job_definition_id   UUID NOT NULL
        REFERENCES job_definitions(id),

    trigger_type        VARCHAR(20) NOT NULL
        CHECK (trigger_type IN (
            'SCHEDULED', 'MANUAL', 'DEPENDENCY', 'RETRY'
        )),

    -- Set for a scheduled occurrence; NULL for manual triggers.
    scheduled_for       TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN (
            'QUEUED', 'WAITING_DEPENDENCY', 'SUBMITTING',
            'RUNNING', 'RETRY_WAIT', 'SUCCEEDED',
            'FAILED', 'CANCELLED', 'SKIPPED'
        )),

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,

    error_message       TEXT
);

CREATE INDEX idx_executions_status
    ON job_executions (status, created_at);

CREATE INDEX idx_executions_job_created
    ON job_executions (job_definition_id, created_at DESC);

CREATE TABLE job_attempts (
    id                  UUID PRIMARY KEY,
    execution_id        UUID NOT NULL
        REFERENCES job_executions(id) ON DELETE CASCADE,

    attempt_number      INTEGER NOT NULL CHECK (attempt_number >= 1),
    jes_job_id          VARCHAR(8),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING', 'SUBMITTED', 'RUNNING',
            'SUCCEEDED', 'FAILED', 'UNKNOWN'
        )),

    return_code         INTEGER,
    submitted_at        TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,

    UNIQUE (execution_id, attempt_number)
);

CREATE UNIQUE INDEX idx_job_attempts_jes_id
    ON job_attempts (jes_job_id)
    WHERE jes_job_id IS NOT NULL;

CREATE INDEX idx_job_attempts_active
    ON job_attempts (status)
    WHERE status IN ('SUBMITTED', 'RUNNING', 'UNKNOWN');

CREATE TABLE job_execution_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id    UUID NOT NULL
        REFERENCES job_executions(id) ON DELETE CASCADE,
    attempt_id      UUID
        REFERENCES job_attempts(id) ON DELETE SET NULL,

    event_type      VARCHAR(40) NOT NULL,
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_execution_events_history
    ON job_execution_events (execution_id, created_at);

-- Prevent duplicate executions for the same scheduled occurrence.
-- Manual/dependency-triggered executions have no schedule occurrence key.
ALTER TABLE job_executions
    ADD COLUMN schedule_id UUID
        REFERENCES job_schedules(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_job_execution_scheduled_occurrence
    ON job_executions (schedule_id, scheduled_for)
    WHERE schedule_id IS NOT NULL
      AND scheduled_for IS NOT NULL;
