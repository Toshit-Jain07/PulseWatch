package com.toshit.PulseWatch.services;

import com.toshit.PulseWatch.dto.request.MetricsWindow;
import com.toshit.PulseWatch.dto.response.MetricsResponse;
import com.toshit.PulseWatch.entity.Monitor;
import com.toshit.PulseWatch.repository.IncidentRepository;
import com.toshit.PulseWatch.repository.MonitorCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MonitorService monitorService;
    private final IncidentRepository incidentRepository;
    private final MonitorCheckRepository monitorCheckRepository;

    public MetricsResponse getMetrics(Long userId, Long monitorId, MetricsWindow window) {
        Monitor monitor = monitorService.findOwnedMonitor(userId, monitorId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minus(window.getDuration());

        // a monitor younger than the window must not be measured against time before it existed
        LocalDateTime start = (monitor.getCreatedAt() != null && monitor.getCreatedAt().isAfter(windowStart))
                ? monitor.getCreatedAt() : windowStart;

        long windowSeconds = Duration.between(start, now).getSeconds();

        IncidentRepository.IncidentStats incidents = incidentRepository.getStats(monitorId, start, now);
        MonitorCheckRepository.ResponseTimeStats timing =
                monitorCheckRepository.getResponseTimeStats(monitorId, start, now);

        long downtime = Math.min(incidents.getDowntimeSeconds().longValue(), Math.max(windowSeconds, 0));

        Double uptime = null;
        if (windowSeconds > 0) {
            uptime = Math.round(100.0 * (1 - (double) downtime / windowSeconds) * 1000) / 1000.0;
        }

        return new MetricsResponse(
                window.getLabel(),
                start,
                now,
                uptime,
                downtime,
                incidents.getIncidentCount(),
                toDouble(incidents.getMttrSeconds()),
                toDouble(timing.getAvgMs()),
                toDouble(timing.getP95Ms())
        );
    }

    private Double toDouble(Number n) {
        return n == null ? null : n.doubleValue();
    }
}