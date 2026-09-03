package com.toshit.PulseWatch.controller;

import com.toshit.PulseWatch.dto.request.MonitorRequest;
import com.toshit.PulseWatch.dto.response.MonitorResponse;
import com.toshit.PulseWatch.services.MonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monitors")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @Valid @RequestBody MonitorRequest request,Authentication authentication){
        Long userId = extractUserId(authentication);
        MonitorResponse response = monitorService.createMonitor(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MonitorResponse>> getMonitors(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(monitorService.getMonitorsForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(monitorService.getMonitorById(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(monitorService.updateMonitor(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        monitorService.deleteMonitor(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}