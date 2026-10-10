package com.toshit.PulseWatch.services;

import com.toshit.PulseWatch.dto.response.IncidentResponse;
import com.toshit.PulseWatch.entity.Incident;
import com.toshit.PulseWatch.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final MonitorService monitorService;

    public List<IncidentResponse> getIncidentsForMonitor(Long userId, Long monitorId) {
        monitorService.findOwnedMonitor(userId, monitorId);   // throws if not the owner

        return incidentRepository.findByMonitorIdOrderByStartedAtDesc(monitorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private IncidentResponse toResponse(Incident incident) {
        Long duration = incident.isResolved()
                ? Duration.between(incident.getStartedAt(), incident.getResolvedAt()).getSeconds()
                : null;

        return new IncidentResponse(
                incident.getId(),
                incident.getStartedAt(),
                incident.getResolvedAt(),
                incident.isResolved(),
                incident.getFailureReason(),
                duration
        );
    }
}