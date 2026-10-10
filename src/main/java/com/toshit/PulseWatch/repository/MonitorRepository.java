package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByUserId(Long userId);

    @Query(value = """
    SELECT * FROM monitors m
    WHERE m.last_checked_at IS NULL
       OR m.last_checked_at <= DATE_SUB(:now, INTERVAL m.check_interval_seconds SECOND)
    """, nativeQuery = true)
    List<Monitor> findDueMonitors(@Param("now") LocalDateTime now);

    @Query("SELECT m.user.email FROM Monitor m WHERE m.id = :monitorId")
    String findOwnerEmail(@Param("monitorId") Long monitorId);
}