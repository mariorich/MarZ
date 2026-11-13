-- Allow max_attempts = 0 to represent unlimited attempts.
ALTER TABLE job_definitions
    DROP CONSTRAINT IF EXISTS job_definitions_max_attempts_check;

ALTER TABLE job_definitions
    ADD CONSTRAINT job_definitions_max_attempts_check
    CHECK (max_attempts >= 0);
