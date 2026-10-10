package com.toshit.PulseWatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private boolean resolved;
    private String failureReason;
    private Long durationSeconds;   // null while the incident is still open
}