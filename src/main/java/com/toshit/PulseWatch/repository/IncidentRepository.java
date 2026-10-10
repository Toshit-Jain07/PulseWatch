package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByMonitorIdAndResolvedFalse(Long monitorId);
    List<Incident> findByMonitorIdOrderByStartedAtDesc(Long monitorId);
}