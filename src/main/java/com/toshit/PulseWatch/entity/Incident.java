package com.toshit.PulseWatch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents",
        indexes = @Index(name = "idx_incidents_monitor_started_at", columnList = "monitor_id, started_at"))
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "failure_reason")
    private String failureReason;

    public void resolve(LocalDateTime when) {
        this.resolved = true;
        this.resolvedAt = when;
    }
}
