package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.MonitorCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {

    interface ResponseTimeStats {
        Number getAvgMs();
        Number getP95Ms();
    }

    @Query(value = """
    SELECT AVG(response_time_ms) AS avgMs,
           MAX(CASE WHEN rn <= CEIL(0.95 * cnt) THEN response_time_ms END) AS p95Ms
    FROM (
        SELECT response_time_ms,
               ROW_NUMBER() OVER (ORDER BY response_time_ms) AS rn,
               COUNT(*) OVER () AS cnt
        FROM monitor_checks
        WHERE monitor_id = :monitorId
          AND success = true
          AND checked_at >= :windowStart
          AND checked_at <= :now
    ) t
    """, nativeQuery = true)
    ResponseTimeStats getResponseTimeStats(@Param("monitorId") Long monitorId,
                                           @Param("windowStart") LocalDateTime windowStart,
                                           @Param("now") LocalDateTime now);

}