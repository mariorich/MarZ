package com.marz.scheduler.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.marz.scheduler.config.ZosmfProperties;

import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.URLEncoder;

@Service
public class ZosmfJobSubmissionService {

    private static final Pattern DATASET_PATTERN =
            Pattern.compile("[A-Za-z0-9@$#-]+(\\.[A-Za-z0-9@$#-]+)*");

    private static final Pattern MEMBER_PATTERN =
            Pattern.compile("[A-Za-z0-9@$#]{1,8}");

    private final HttpClient httpClient;
    private final ZosmfProperties properties;
    private final ObjectMapper objectMapper;

    public ZosmfJobSubmissionService(
            HttpClient zosmfHttpClient,
            ZosmfProperties properties,
            ObjectMapper objectMapper) {
        this.httpClient = zosmfHttpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    
public int testConnection() {
    String baseUrl = properties.getBaseUrl();
    String username = properties.getUsername();
    String password = properties.getPassword();

    if (baseUrl == null || baseUrl.isBlank()
            || username == null || username.isBlank()
            || password == null || password.isBlank()) {
        throw new IllegalStateException(
                "z/OSMF URL or credentials are not configured.");
    }

    try {
        String endpoint = baseUrl.replaceAll("/+$", "")
                + "/zosmf/restjobs/jobs?owner="
                + URLEncoder.encode(
                        username, StandardCharsets.UTF_8)
                + "&prefix=*";

        String credentials = username + ":" + password;
        String basicAuth = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));

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

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            throw new ZosmfSubmissionException(
                    "z/OSMF connection test failed. HTTP status: "
                            + status);
        }

        return status;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZosmfSubmissionException(
                    "Connection test was interrupted.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new ZosmfSubmissionException(
                    "Could not connect to z/OSMF: " + e.getMessage(), e);
        }
    }


    public JsonNode submitDatasetMember(String dataset, String member) {
        validateDatasetAndMember(dataset, member);

        String baseUrl = properties.getBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "ZOSMF_BASE_URL is not configured");
        }

        if (properties.getUsername() == null
                || properties.getUsername().isBlank()
                || properties.getPassword() == null
                || properties.getPassword().isBlank()) {
            throw new IllegalStateException(
                    "z/OSMF credentials are not configured");
        }

        try {
            String fileReference =
                    "//'" + dataset + "(" + member + ")'";

            String requestBody = objectMapper
                    .createObjectNode()
                    .put("file", fileReference)
                    .toString();

            String credentials = properties.getUsername()
                    + ":" + properties.getPassword();

            String basicAuth = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8));

            String endpoint = baseUrl.replaceAll("/+$", "")
                    + "/zosmf/restjobs/jobs";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(
                            properties.getReadTimeoutSeconds()))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                    .PUT(HttpRequest.BodyPublishers.ofString(
                            requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new ZosmfSubmissionException(
                        "z/OSMF rejected the submission. HTTP status: "
                                + statusCode
                                + ". Response: "
                                + response.body());
            }

            JsonNode result = objectMapper.readTree(response.body());

            if (result.path("jobid").asText("").isBlank()) {
                throw new ZosmfSubmissionException(
                        "z/OSMF returned a successful HTTP response "
                                + "without a job ID.");
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZosmfSubmissionException(
                    "Interrupted while submitting the job.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new ZosmfSubmissionException(
                    "Could not submit the job to z/OSMF: "
                            + e.getMessage(), e);
        }
    }

    private void validateDatasetAndMember(
            String dataset, String member) {
        if (dataset == null
                || !DATASET_PATTERN.matcher(dataset).matches()
                || dataset.length() > 44) {
            throw new IllegalArgumentException(
                    "Invalid JCL dataset name.");
        }

        if (member == null
                || !MEMBER_PATTERN.matcher(member).matches()) {
            throw new IllegalArgumentException(
                    "Invalid JCL member name.");
        }
    }
}
