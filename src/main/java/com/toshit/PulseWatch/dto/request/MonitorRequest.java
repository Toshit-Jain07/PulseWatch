package com.toshit.PulseWatch.dto.request;

import com.toshit.PulseWatch.entity.HttpMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MonitorRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;

    @NotNull(message = "HTTP method is required")
    private HttpMethod httpMethod;

    @NotNull(message = "Expected status code is required")
    @Min(value = 100, message = "Status code must be a valid HTTP status")
    @Max(value = 599, message = "Status code must be a valid HTTP status")
    private Integer expectedStatusCode;

    @NotNull(message = "Check interval is required")
    @Min(value = 30, message = "Check interval must be at least 10 seconds")
    private Integer checkIntervalSeconds;

    @NotNull(message = "Timeout is required")
    @Min(value = 100, message = "Timeout must be at least 100ms")
    private Integer timeoutMs;
}