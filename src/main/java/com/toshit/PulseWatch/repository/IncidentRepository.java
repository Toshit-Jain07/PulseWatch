package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByMonitorIdAndResolvedFalse(Long monitorId);

    List<Incident> findByMonitorIdOrderByStartedAtDesc(Long monitorId);

    interface IncidentStats {
        Long getIncidentCount();
        Number getDowntimeSeconds();
        Number getMttrSeconds();      // null if nothing resolved in the window
    }

    @Query(value = """
        SELECT COUNT(*) AS incidentCount,
               CAST(COALESCE(SUM(TIMESTAMPDIFF(SECOND,
                    GREATEST(started_at, :windowStart),
                    LEAST(COALESCE(resolved_at, :now), :now))), 0) AS SIGNED) AS downtimeSeconds,
               AVG(CASE WHEN resolved_at IS NOT NULL AND resolved_at >= :windowStart
                        THEN TIMESTAMPDIFF(SECOND, started_at, resolved_at) END) AS mttrSeconds
        FROM incidents
        WHERE monitor_id = :monitorId
          AND started_at < :now
          AND COALESCE(resolved_at, :now) > :windowStart
        """, nativeQuery = true)
    IncidentStats getStats(@Param("monitorId") Long monitorId,
                           @Param("windowStart") LocalDateTime windowStart,
                           @Param("now") LocalDateTime now);
}