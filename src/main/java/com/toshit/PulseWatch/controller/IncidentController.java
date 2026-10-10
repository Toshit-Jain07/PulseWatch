package com.toshit.PulseWatch.controller;


import com.toshit.PulseWatch.dto.response.IncidentResponse;
import com.toshit.PulseWatch.services.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitors/{monitorId}/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getIncidents(
            @PathVariable Long monitorId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getDetails();
        return ResponseEntity.ok(incidentService.getIncidentsForMonitor(userId, monitorId));
    }
}
