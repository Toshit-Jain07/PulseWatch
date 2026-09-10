package com.toshit.PulseWatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitor_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        this.checkedAt = LocalDateTime.now();
    }
}