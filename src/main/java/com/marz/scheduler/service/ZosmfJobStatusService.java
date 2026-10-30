package com.marz.scheduler.service;

import com.marz.scheduler.config.ZosmfProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ZosmfJobStatusService {

    private static final Pattern JOB_NAME_PATTERN =
            Pattern.compile("[A-Za-z0-9@$#]{1,8}");

    private static final Pattern JOB_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9@$#]{1,8}");

    private final HttpClient httpClient;
    private final ZosmfProperties properties;
    private final ObjectMapper objectMapper;

    public ZosmfJobStatusService(
            HttpClient zosmfHttpClient,
            ZosmfProperties properties,
            ObjectMapper objectMapper) {

        this.httpClient = zosmfHttpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode getJobStatus(String jobName, String jobId) {

        if (jobName == null ||
                !JOB_NAME_PATTERN.matcher(jobName).matches()) {
            throw new IllegalArgumentException("Invalid JES job name.");
        }

        if (jobId == null ||
                !JOB_ID_PATTERN.matcher(jobId).matches()) {
            throw new IllegalArgumentException("Invalid JES job ID.");
        }

        String baseUrl = properties.getBaseUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "ZOSMF_BASE_URL is not configured.");
        }

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "z/OSMF credentials are not configured.");
        }

        try {
            String credentials = username + ":" + password;

            String basicAuth = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8));

            String endpoint = baseUrl.replaceAll("/+$", "")
                    + "/zosmf/restjobs/jobs/"
                    + URLEncoder.encode(
                            jobName, StandardCharsets.UTF_8)
                    + "/"
                    + URLEncoder.encode(
                            jobId, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(
                            properties.getReadTimeoutSeconds()))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new ZosmfSubmissionException(
                        "Could not retrieve JES job status. HTTP status: "
                                + statusCode
                                + ". Response: "
                                + response.body());
            }

            return objectMapper.readTree(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ZosmfSubmissionException(
                    "Interrupted while retrieving JES job status.", e);

        } catch (IOException e) {
            throw new ZosmfSubmissionException(
                    "Could not retrieve JES job status: "
                            + e.getMessage(), e);
        }
    }
}
