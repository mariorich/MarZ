package com.marz.scheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateJobDefinitionRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank
    @Size(max = 255)
    private String jclDataset;

    @NotBlank
    @Size(max = 255)
    private String jclMember;

    @NotNull
    @Min(1)
    private Integer maxAttempts = 1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJclDataset() {
        return jclDataset;
    }

    public void setJclDataset(String jclDataset) {
        this.jclDataset = jclDataset;
    }

    public String getJclMember() {
        return jclMember;
    }

    public void setJclMember(String jclMember) {
        this.jclMember = jclMember;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}