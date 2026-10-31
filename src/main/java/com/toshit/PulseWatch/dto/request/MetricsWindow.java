package com.toshit.PulseWatch.dto.request;
import java.time.Duration;
import java.util.Optional;

public enum MetricsWindow {
    HOURS_24("24h", Duration.ofHours(24)),
    DAYS_7("7d", Duration.ofDays(7)),
    DAYS_30("30d", Duration.ofDays(30));

    private final String label;
    private final Duration duration;

    MetricsWindow(String label, Duration duration) {
        this.label = label;
        this.duration = duration;
    }

    public String getLabel() { return label; }
    public Duration getDuration() { return duration; }

    public static Optional<MetricsWindow> parse(String value) {
        for (MetricsWindow w : values()) {
            if (w.label.equalsIgnoreCase(value)) return Optional.of(w);
        }
        return Optional.empty();
    }
}