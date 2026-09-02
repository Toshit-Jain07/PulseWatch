package com.toshit.PulseWatch.dto.response;

import com.toshit.PulseWatch.entity.HttpMethod;
import com.toshit.PulseWatch.entity.MonitorStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorResponse {

    private Long id;
    private String name;
    private String url;
    private HttpMethod httpMethod;
    private Integer expectedStatusCode;
    private Integer checkIntervalSeconds;
    private Integer timeoutMs;
    private MonitorStatus status;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime createdAt;
}