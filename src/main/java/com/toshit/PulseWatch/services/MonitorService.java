package com.toshit.PulseWatch.services;

import com.toshit.PulseWatch.dto.request.MonitorRequest;
import com.toshit.PulseWatch.dto.response.MonitorResponse;
import com.toshit.PulseWatch.entity.Monitor;
import com.toshit.PulseWatch.entity.User;
import com.toshit.PulseWatch.repository.MonitorRepository;
import com.toshit.PulseWatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final UserRepository userRepository;

    public MonitorResponse createMonitor(Long userId, MonitorRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Monitor monitor = new Monitor();
        monitor.setUser(user);
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setHttpMethod(request.getHttpMethod());
        monitor.setExpectedStatusCode(request.getExpectedStatusCode());
        monitor.setCheckIntervalSeconds(request.getCheckIntervalSeconds());
        monitor.setTimeoutMs(request.getTimeoutMs());

        Monitor saved = monitorRepository.save(monitor);
        return toResponse(saved);
    }

    public List<MonitorResponse> getMonitorsForUser(Long userId) {
        return monitorRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MonitorResponse getMonitorById(Long userId, Long monitorId) {
        Monitor monitor = findOwnedMonitor(userId, monitorId);
        return toResponse(monitor);
    }

    public MonitorResponse updateMonitor(Long userId, Long monitorId, MonitorRequest request) {
        Monitor monitor = findOwnedMonitor(userId, monitorId);

        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setHttpMethod(request.getHttpMethod());
        monitor.setExpectedStatusCode(request.getExpectedStatusCode());
        monitor.setCheckIntervalSeconds(request.getCheckIntervalSeconds());
        monitor.setTimeoutMs(request.getTimeoutMs());

        Monitor updated = monitorRepository.save(monitor);
        return toResponse(updated);
    }

    public void deleteMonitor(Long userId, Long monitorId) {
        Monitor monitor = findOwnedMonitor(userId, monitorId);
        monitorRepository.delete(monitor);
    }

    public Monitor findOwnedMonitor(Long userId, Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new RuntimeException("Monitor not found"));

        if (!monitor.getUser().getId().equals(userId)) {
            throw new RuntimeException("You do not own this monitor");
        }

        return monitor;
    }

    private MonitorResponse toResponse(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getHttpMethod(),
                monitor.getExpectedStatusCode(),
                monitor.getCheckIntervalSeconds(),
                monitor.getTimeoutMs(),
                monitor.getStatus(),
                monitor.getLastCheckedAt(),
                monitor.getCreatedAt()
        );
    }
}