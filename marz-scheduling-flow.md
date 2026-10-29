# MarZ Scheduling Flow

*Last updated: 2026-10-29*

## 1. Purpose

MarZ is a custom z/OS job scheduling and monitoring application built
with Spring Boot, PostgreSQL, Spring Data JPA, and Flyway. The
scheduling engine is custom-built; Quartz is not used.

This document describes the scheduling and execution flow implemented so
far, followed by the planned z/OS integration.

## 2. High-level flow

``` text
Job definition + schedule
          |
          v
DueScheduleProcessor (polls every 5 seconds by default)
          |
          | Finds enabled schedules whose next_run_at <= current time
          v
Create job_execution with status QUEUED
          |
          | Advance schedule.next_run_at to its next cron occurrence
          v
QueuedExecutionProcessor (polls every 2 seconds by default)
          |
          | Claims queued executions using PostgreSQL row locks
          v
Create job_attempt (attempt_number = 1, status = PENDING)
          |
          | Update execution status
          v
job_execution status = SUBMITTING
          |
          v
[Planned] Submit JCL through Zowe to z/OS
          |
          v
[Planned] Store JES job ID and poll job status / return code
          |
          v
[Planned] Finish as SUCCEEDED or FAILED; apply retry policy if needed
```

The first two processing stages have been tested successfully. Zowe
submission and JES monitoring are not implemented yet.

## 3. Main database records

### `job_definitions`

Defines a job that MarZ can schedule. Relevant fields include:

-   `id`: job definition UUID.
-   `name`: unique name.
-   `jcl_dataset`: dataset containing the JCL.
-   `jcl_member`: member to submit.
-   `enabled`: whether the job definition is enabled.
-   `max_attempts`: configured retry/attempt limit.

### `job_schedules`

Defines when a job should run. Relevant fields include:

-   `job_definition_id`: job definition to run.
-   `cron_expression`: Spring cron expression.
-   `timezone`: timezone used to interpret the schedule.
-   `enabled`: whether the schedule is enabled.
-   `next_run_at`: next planned occurrence.
-   `last_run_at`: last-run tracking field.

### `job_executions`

Represents one scheduled or manually triggered run of a job. Relevant
fields include:

-   `job_definition_id`: job to execute.
-   `schedule_id`: originating schedule, when applicable.
-   `trigger_type`: for example, `SCHEDULED` or `MANUAL`.
-   `scheduled_for`: scheduled occurrence time.
-   `status`: lifecycle state.
-   `created_at`, `started_at`, `finished_at`: execution timestamps.
-   `error_message`: error details, if applicable.

Current execution statuses are:

-   `QUEUED`
-   `WAITING_DEPENDENCY`
-   `SUBMITTING`
-   `RUNNING`
-   `RETRY_WAIT`
-   `SUCCEEDED`
-   `FAILED`
-   `CANCELLED`
-   `SKIPPED`

### `job_attempts`

Represents an individual attempt to submit/run an execution. Relevant
fields include:

-   `execution_id`: parent execution.
-   `attempt_number`: starts at `1`.
-   `jes_job_id`: JES job identifier returned by z/OS, once submission
    is implemented.
-   `status`: attempt status.
-   `return_code`: job return code, once known.
-   `submitted_at`, `started_at`, `finished_at`: attempt timestamps.

Allowed attempt statuses in the current database constraint are:

-   `PENDING`
-   `SUBMITTED`
-   `RUNNING`
-   `SUCCEEDED`
-   `FAILED`
-   `UNKNOWN`

### `job_execution_events`

Stores events associated with an execution, optionally linked to an
attempt. Event logging is not yet part of the initial worker flow
described here.

## 4. Stage A --- Define a job and schedule

1.  Create a job definition through the job API (`/api/jobs`).
2.  Create a schedule through the schedule API (`/api/schedules`).
3.  The schedule service validates the cron expression and timezone.
4.  It calculates the first `next_run_at` occurrence and persists the
    schedule.

The schedule and job definition must both be enabled for the scheduler
to process the schedule.

## 5. Stage B --- Detect due schedules

`DueScheduleProcessor` is a Spring scheduled service. Its default
polling interval is configured by:

``` properties
marz.scheduler.poll-interval-ms=5000
```

On each poll, it:

1.  Starts a database transaction.
2.  Queries enabled schedules whose `next_run_at` is not null and is
    less than or equal to the current time.
3.  Checks that the associated job definition is enabled.
4.  Locks due schedule rows using `FOR UPDATE ... SKIP LOCKED`, so
    concurrent scheduler instances can avoid claiming the same schedule
    row.
5.  Creates a `job_executions` row with:
    -   `trigger_type = SCHEDULED`
    -   `status = QUEUED`
    -   `scheduled_for` set to the occurrence that was due
    -   the associated job and schedule IDs
6.  Calculates and saves the next cron occurrence in
    `job_schedules.next_run_at`.
7.  Commits the transaction.

The execution creation and schedule advancement happen in the same
transaction. This helps prevent the database from recording only one
side of that update.

### Missed schedules

The current processor calculates the next occurrence from the current
time. This means it skips older missed occurrences after downtime
instead of creating a separate execution for every missed cron
occurrence.

## 6. Stage C --- Claim queued executions

`QueuedExecutionProcessor` is a Spring scheduled service. Its default
polling interval is configured by:

``` properties
marz.worker.poll-interval-ms=2000
```

On each poll, it:

1.  Starts a database transaction.
2.  Selects up to 10 executions with `status = QUEUED`, ordered oldest
    first.
3.  Uses `FOR UPDATE SKIP LOCKED` to avoid two workers claiming the same
    execution at the same time.
4.  Creates a `job_attempts` row for each claimed execution:
    -   `attempt_number = 1`
    -   `status = PENDING`
    -   `execution_id` set to the claimed execution ID
5.  Changes the execution status to `SUBMITTING`.
6.  Saves the records and commits the transaction.

Creating the attempt and updating the execution status are performed in
the same transaction. If the transaction fails, both changes should roll
back together.

### Current limitation

At this stage, `SUBMITTING` means that the worker has claimed the
execution and prepared its first attempt. It does **not** mean that JCL
has actually been submitted to z/OS. Until Zowe submission is
integrated, executions can remain in `SUBMITTING` and attempts can
remain `PENDING`.

## 7. Stage D --- Planned Zowe submission and monitoring

The next planned stage is to connect the worker to the existing
Zowe/z/OS environment.

The intended flow is:

1.  Read `jcl_dataset` and `jcl_member` from the job definition.
2.  Submit the requested JCL through an appropriate Zowe z/OS interface.
3.  Capture the JES job ID returned by z/OS.
4.  Update the attempt with `jes_job_id`, `submitted_at`, and
    `status = SUBMITTED`.
5.  Poll or otherwise retrieve the JES job status.
6.  Update the attempt and execution as the job runs and finishes.
7.  Store the final return code and timestamps.
8.  Mark the execution `SUCCEEDED` or `FAILED` according to the job
    result and the application's agreed return-code policy.

Network calls to Zowe should be performed outside the database
transaction used to claim queued executions. This avoids holding
PostgreSQL row locks while waiting for remote calls. The implementation
will also need explicit handling for timeouts and ambiguous submission
outcomes, because a request can time out even if z/OS accepted the job.

## 8. Planned retry behavior

The schema includes `max_attempts`, and the execution statuses include
`RETRY_WAIT`. Retry processing has not been implemented in the worker
described above.

When retries are added, the intended model is:

-   Keep one `job_executions` row for the logical run.
-   Add another `job_attempts` row for each retry, incrementing
    `attempt_number`.
-   Respect the job definition's `max_attempts`.
-   Record the reason and timing for retrying.
-   Mark the execution `FAILED` when no further attempt is permitted.

Retry handling must also avoid creating duplicate submissions if the
outcome of a previous Zowe submission is unknown.

## 9. Testing the current scheduler

To trigger an existing schedule immediately in a development
environment, update its `next_run_at`:

``` sql
UPDATE job_schedules
SET next_run_at = NOW() - INTERVAL '1 minute'
WHERE id = 'YOUR-SCHEDULE-UUID';
```

The schedule and its job definition must be enabled. After the scheduler
and worker have polled, inspect recent executions:

``` sql
SELECT id, job_definition_id, status, created_at
FROM job_executions
ORDER BY created_at DESC
LIMIT 10;
```

Inspect attempts:

``` sql
SELECT execution_id, attempt_number, status, jes_job_id
FROM job_attempts
ORDER BY attempt_number DESC;
```

For the current implemented flow, a processed execution is expected to
have `status = SUBMITTING`, with an attempt whose `attempt_number = 1`,
`status = PENDING`, and `jes_job_id` still null.

Changing `next_run_at` as shown above is a test technique; do not use it
on production schedules without understanding the impact.

## 10. Current implementation status

  Capability                                    Status
  --------------------------------------------- -----------------------------------
  Job definition API and persistence            Implemented
  Schedule API, cron/timezone validation        Implemented
  Due-schedule polling and execution creation   Implemented and tested
  Queue claiming with PostgreSQL row locks      Implemented and tested
  Initial attempt creation                      Implemented and tested
  Actual JCL submission through Zowe            Planned
  JES status and return-code monitoring         Planned
  Retry processing                              Planned
  Full execution event history                  Not yet integrated into this flow

## 11. Design principles

-   PostgreSQL is the source of truth for schedules, executions, and
    attempts.
-   Cron scheduling is implemented by MarZ, not Quartz.
-   Row locking with `SKIP LOCKED` helps coordinate multiple
    scheduler/worker instances.
-   Database state changes that belong together should be committed in
    one transaction.
-   Remote Zowe calls should not hold database row locks open.
-   Secrets and credentials must be kept out of source control and
    documentation.
