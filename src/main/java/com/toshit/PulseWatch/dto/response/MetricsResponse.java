package com.toshit.PulseWatch.dto.response;

import java.time.LocalDateTime;

public record MetricsResponse(
        String window,
        LocalDateTime from,            // effective start: clipped to when the monitor was created
        LocalDateTime to,
        Double uptimePercent,          // null if the monitor has no history yet
        long downtimeSeconds,
        long incidentCount,
        Double mttrSeconds,            // null if no incident was resolved in the window
        Double avgResponseTimeMs,      // null if no successful checks
        Double p95ResponseTimeMs
) {}