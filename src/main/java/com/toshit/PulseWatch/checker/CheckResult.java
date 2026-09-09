package com.toshit.PulseWatch.checker;

public record CheckResult(
        boolean success,
        Integer statusCode,      // null if request never got a response at all
        long responseTimeMs,
        String failureReason     // null if success; e.g. "TIMEOUT", "CONNECTION_REFUSED",
                                // "DNS_FAILURE", "STATUS_MISMATCH"
) {}
