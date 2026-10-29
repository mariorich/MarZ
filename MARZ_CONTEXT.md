# MarZ Project Context

*Last updated: 2026-10-26*

## 1. Purpose

MarZ is a custom z/OS job scheduling and monitoring project. The
intended direction is to build a Spring Boot application with a
PostgreSQL-backed scheduler and integration with the existing IBM Z
environment through Zowe. The scheduler is custom-built; Quartz is not
the planned scheduling engine.

This file is the project handover/context document. Update it as
implementation decisions and project state change.

## 2. Current technology stack

-   **Language:** Java 25
-   **Framework:** Spring Boot
-   **Build:** Maven
-   **Packaging:** executable JAR
-   **Database:** PostgreSQL 17
-   **Persistence:** Spring Data JPA
-   **Database migrations:** Flyway
-   **API:** Spring Web / REST
-   **Validation:** Spring Validation
-   **Monitoring:** Spring Boot Actuator
-   **Development environment:** VS Code in GitHub Codespaces / Dev
    Container
-   **Mainframe integration direction:** Zowe and z/OS interfaces

The intended Spring Initializr dependencies are Spring Web, Spring Data
JPA, PostgreSQL Driver, Validation, Spring Boot Actuator, and Flyway
Migration. Verify `pom.xml` before assuming all dependencies are
present.

## 3. Project metadata

-   **Group:** `com.marz`
-   **Artifact:** `marz-scheduler`
-   **Application name:** `marz-scheduler`
-   **Base Java package:** `com.marz.scheduler`
-   **Main class:** `MarzSchedulerApplication`
-   **Application port:** `8080`

The Java package must use dot-separated identifiers and must not contain
hyphens. The Maven artifact may contain hyphens.

Expected main class path:

``` text
src/main/java/com/marz/scheduler/MarzSchedulerApplication.java
```

Expected main class shape:

``` java
package com.marz.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarzSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarzSchedulerApplication.class, args);
    }
}
```

## 4. Expected project structure

``` text
MarZ/
├── .devcontainer/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/marz/scheduler/
│   │   │       ├── MarzSchedulerApplication.java
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── repository/
│   │   │       ├── scheduler/
│   │   │       ├── service/
│   │   │       └── integration/
│   │   │           └── zowe/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/
│   └── test/
├── compose.yaml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

This is the target/illustrative structure, not confirmation that every
listed package or file has already been created. Create packages as
features are implemented.

## 5. Runtime and Codespaces

-   Spring Boot is intended to listen on port `8080`.
-   The current Codespaces forwarded URL shared during setup was:
    `https://solitary-owl-gpp7666r7x9hvqw9-8080.app.github.dev/`
-   The URL is a forwarded HTTP(S) application endpoint. It is **not**
    the PostgreSQL JDBC URL.
-   A `404 Not Found` at `/` is expected until a controller or page is
    mapped to the root path.
-   Do not hardcode the forwarded Codespaces URL into
    `application.properties`.

The forwarded URL can change between Codespaces sessions or
environments; check the Ports tab for the current URL.

## 6. PostgreSQL / Docker Compose

Known configuration details from setup:

-   PostgreSQL version: 17
-   Database: `zosscheduler`
-   Username: `scheduler`
-   Compose password setting:
    `POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-devpassword}`
-   PostgreSQL service hostname used in the JDBC URL: `postgres`,
    **provided** that this is the actual Compose service name and the
    application container shares its Docker network.
-   The PostgreSQL service was previously reported as healthy, and a
    connection to `postgres:5432` was reported as verified.

When Spring Boot runs in the same Compose network as PostgreSQL, a
likely JDBC URL is:

``` text
jdbc:postgresql://postgres:5432/zosscheduler
```

If Spring Boot runs directly on a host rather than in the Compose
network, use the published host port and `localhost` instead. Confirm
the actual `compose.yaml` before changing the URL.

### Application properties

The following configuration was discussed during setup. Verify the
actual file and Compose environment before relying on it:

``` properties
spring.application.name=marz-scheduler
server.port=8080

spring.datasource.url=${DB_URL:jdbc:postgresql://postgres:5432/zosscheduler}
spring.datasource.username=${DB_USERNAME:scheduler}
spring.datasource.password=${POSTGRES_PASSWORD:devpassword}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false

spring.flyway.enabled=true

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when_authorized

logging.level.com.marz.scheduler=INFO
```

Important notes:

-   Compose service environment variables are **not automatically
    available** inside a separate application container. Pass
    `POSTGRES_PASSWORD` (and any other required variables) to the app
    service explicitly.
-   Avoid committing real credentials. `devpassword` is a development
    fallback only.
-   `spring.jpa.hibernate.ddl-auto=validate` checks that the schema
    matches the entity mappings; it does not create tables.
-   Flyway migrations belong under `src/main/resources/db/migration/`,
    with names such as `V1__create_initial_schema.sql`.
-   Confirm that the required Flyway database support and PostgreSQL
    driver dependencies are present in `pom.xml`.
-   If no schema/migration exists yet, schema validation may prevent
    startup once entities are introduced. Plan the initial migration
    before relying on JPA entities.

## 7. Maven Wrapper

The project was generated through the VS Code Maven extension. The
wrapper scripts were initially missing; wrapper generation was then
attempted.

Expected wrapper files:

``` text
.mvn/wrapper/maven-wrapper.properties
mvnw
mvnw.cmd
```

The exact wrapper implementation/files depend on the Maven Wrapper
version and configuration. Verify that both scripts exist before relying
on the wrapper.

On Linux/Codespaces, from the project root:

``` bash
chmod +x mvnw
./mvnw -version
./mvnw clean package
```

Use `./mvnw spring-boot:run` to run the application if the wrapper is
configured and the project has the necessary Spring Boot plugin.

## 8. Zowe / z/OS configuration

The existing Zowe configuration for MarZ includes profiles for:

-   `zosmf` --- port `10443`
-   `tso` --- account `FB3`, code page `1047`, logon procedure
    `IZUFPROC`
-   `ssh` --- port `22`
-   `rse` --- port `6800`, base path `rseapi`, protocol `https`
-   `cics`
-   `global_base` --- host `204.90.115.200`, with
    `rejectUnauthorized: false`

Defaults were set to the relevant profiles and `autoStore` was `false`.

These are remembered configuration details, not a replacement for the
actual Zowe configuration file. Do not copy credentials or secrets into
this document. `rejectUnauthorized: false` disables certificate
verification and should be reviewed carefully before production use.

## 9. Current progress

Reported as completed or established:

-   Spring Boot/Maven project generated using the VS Code Maven
    extension.
-   Project intended to use Java 25.
-   Main class renamed from `Main` to `MarzSchedulerApplication`.
-   `application.properties` was created/configured.
-   Maven Wrapper generation was worked through; confirm the scripts are
    present.
-   Spring Boot started and was exposed on Codespaces port `8080`.
-   The initial root URL returned Spring Boot's Whitelabel 404 page
    because no root mapping existed.

Not yet confirmed:

-   That all intended dependencies are present in `pom.xml`.
-   That the current `application.properties` exactly matches the
    configuration above.
-   That the root `HomeController` discussed in conversation was
    implemented. The proposed example used `@RestController` and
    returned an HTML string, which would be returned as plain text
    unless the response content type is set to HTML. For a real UI, use
    a template approach such as Thymeleaf or return a proper HTML
    response.
-   That Flyway migrations or scheduler database tables exist in the
    generated Spring project.
-   That the Spring Boot application has successfully connected to
    PostgreSQL during its current run.

## 10. Suggested next steps

1.  Inspect `pom.xml` and confirm the Spring Boot parent/plugin, Java 25
    configuration, and intended dependencies.
2.  Confirm `mvnw`, `mvnw.cmd`, and
    `.mvn/wrapper/maven-wrapper.properties` exist and that
    `./mvnw -version` works.
3.  Check `compose.yaml` for the actual PostgreSQL service name,
    network, port publishing, database/user variables, and app-service
    environment.
4.  Verify the actual `application.properties` and start the
    application.
5.  Add a simple health endpoint or root page and test it through
    Codespaces port `8080`.
6.  Design the initial PostgreSQL schema and Flyway migration before
    introducing JPA entities with `ddl-auto=validate`.
7.  Build the scheduler's domain model, API, execution history and
    scheduling logic incrementally.
8.  Add Zowe/mainframe execution integration after the core API and
    persistence layer are stable.

## 11. Working conventions

-   Treat this file as a maintained project handover document; update it
    when architecture, dependencies, database schema, endpoints, or
    implementation status changes.
-   Distinguish planned structure from files that have actually been
    created.
-   Do not assume a command or integration succeeded unless its output
    confirms success.
-   Keep secrets out of source control and this document.
