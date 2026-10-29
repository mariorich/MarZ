package com.marz.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MarzSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarzSchedulerApplication.class, args);
    }
}