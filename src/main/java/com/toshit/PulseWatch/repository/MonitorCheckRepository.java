package com.toshit.PulseWatch.repository;

import com.toshit.PulseWatch.entity.MonitorCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {


}