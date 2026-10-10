package com.toshit.PulseWatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod httpMethod;

    @Column(name = "expected_status_code", nullable = false)
    private Integer expectedStatusCode;

    @Column(name = "check_interval_seconds", nullable = false)
    private Integer checkIntervalSeconds;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "consecutive_successes", nullable = false)
    private Integer consecutiveSuccesses = 0;

    @Column(name = "failure_streak_started_at")
    private LocalDateTime failureStreakStartedAt;

    @Column(name = "success_streak_started_at")
    private LocalDateTime successStreakStartedAt;

    @Column(name = "alert_active", nullable = false)
    private boolean alertActive = false;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MonitorStatus.PENDING;
        }
    }
}