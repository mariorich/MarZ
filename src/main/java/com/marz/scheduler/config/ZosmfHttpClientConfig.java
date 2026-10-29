package com.marz.scheduler.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZosmfHttpClientConfig {

    @Bean
    public HttpClient zosmfHttpClient(ZosmfProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(
                        properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
