
# MarZ – Mainframe Job Scheduler

**MarZ** is a personal project focused on building a modern job scheduling and monitoring platform for IBM z/OS environments.

The goal is to provide a centralised way to define, schedule and manage batch jobs on a mainframe, combining modern application technologies with IBM's z/OS ecosystem.

## Overview

MarZ is designed to bridge the gap between modern application development and traditional mainframe batch processing.

It communicates with **IBM z/OS through z/OSMF REST APIs**, providing an integration point for submitting batch jobs to JES (Job Entry Subsystem) and retrieving job information.

The project focuses on:

- **Job scheduling:** Define recurring job schedules using cron expressions and configurable time zones.
- **Execution management:** Create and track individual job executions, including their status and execution history.
- **Mainframe integration:** Submit JCL members from partitioned datasets through z/OSMF.
- **JES monitoring:** Retrieve job information, including JES status, job ID and return code.
- **Persistent storage:** Store job definitions, schedules and execution information in PostgreSQL.

## Mainframe Integration

A core aspect of MarZ is its integration with IBM z/OS.

Rather than relying on scripts running directly on the mainframe to communicate with external services, MarZ uses the z/OSMF REST interface.

The intended workflow is:

1. MarZ identifies a job that is due for execution.
2. The application submits the corresponding JCL member through z/OSMF.
3. z/OSMF submits the job to JES.
4. MarZ retrieves JES job information and monitors its execution.
5. The execution result and return code are stored in the application database.

The z/OSMF submission and JES status retrieval services have been implemented and tested. Integrating submission and automatic JES monitoring into the scheduler's complete execution lifecycle is ongoing.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 25 | Core application development |
| Spring Boot 4 | Application framework and REST services |
| Spring Data JPA | Database persistence |
| PostgreSQL | Relational database |
| Flyway | Database versioning and migrations |
| IBM z/OSMF REST API | Mainframe communication |
| JES2 | Batch job execution and status information |
| Maven | Build and dependency management |

## Architecture

MarZ follows a layered Spring Boot architecture, separating scheduling, execution processing, persistence and mainframe communication.

Background processes identify due schedules and process queued executions. PostgreSQL provides persistent storage, while z/OSMF acts as the integration point with the mainframe environment.

## Project Status

MarZ is an ongoing personal development project.

The current implementation includes the scheduling foundation, persistent execution management, PostgreSQL integration, and tested services for z/OSMF job submission and JES status retrieval.

Current development is focused on connecting these capabilities into the complete execution lifecycle, including automatic job submission, monitoring and result handling.

---

*Built as a practical exploration of Java, Spring Boot and IBM z/OS integration, combining modern backend development with mainframe technologies.*
