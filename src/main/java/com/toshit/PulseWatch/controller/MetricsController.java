package com.toshit.PulseWatch.controller;

import com.toshit.PulseWatch.dto.request.MetricsWindow;
import com.toshit.PulseWatch.dto.response.MetricsResponse;
import com.toshit.PulseWatch.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/monitors/{monitorId}/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    public ResponseEntity<MetricsResponse> getMetrics(
            @PathVariable Long monitorId,
            @RequestParam(defaultValue = "24h") String window,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getDetails();
        MetricsWindow parsed = MetricsWindow.parse(window)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "window must be one of: 24h, 7d, 30d"));
        return ResponseEntity.ok(metricsService.getMetrics(userId, monitorId, parsed));
    }
}
