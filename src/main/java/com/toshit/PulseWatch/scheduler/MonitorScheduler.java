package com.toshit.PulseWatch.scheduler;

import com.toshit.PulseWatch.entity.Incident;
import com.toshit.PulseWatch.checker.CheckResult;
import com.toshit.PulseWatch.checker.MonitorCheckService;
import com.toshit.PulseWatch.entity.Monitor;
import com.toshit.PulseWatch.entity.MonitorCheck;
import com.toshit.PulseWatch.entity.MonitorStatus;
import com.toshit.PulseWatch.repository.IncidentRepository;
import com.toshit.PulseWatch.repository.MonitorCheckRepository;
import com.toshit.PulseWatch.repository.MonitorRepository;
import com.toshit.PulseWatch.services.EmailService;
import com.toshit.PulseWatch.services.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckService monitorCheckService;
    private final MonitorCheckRepository monitorCheckRepository;
    private final IncidentRepository incidentRepository;

    private final EmailService emailService;

    private static final Duration RECOVERY_CONFIRMATION = Duration.ofMinutes(1);
    private static final Duration REMINDER_INTERVAL = Duration.ofHours(1);

    private final RedisLockService redisLockService;

    @Scheduled(fixedRate = 10000)
    public void checkDueMonitors() {
        List<Monitor> dueMonitors = monitorRepository.findDueMonitors(LocalDateTime.now());
        log.info("Scheduler tick: {} monitor(s) due for a check", dueMonitors.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Monitor monitor : dueMonitors) {
                executor.submit(() -> {
                    try {
                        checkMonitor(monitor);
                    } catch (Exception e) {
                        log.error("Check failed for monitor {}", monitor.getId(), e);
                    }
                });
            }
        }
    }

    private void checkMonitor(Monitor staleMonitor) {
        String lockKey = "lock:monitor:" + staleMonitor.getId();
        // lock must outlive the longest possible check (timeout) plus a buffer
        Duration ttl = Duration.ofMillis(staleMonitor.getTimeoutMs()).plusSeconds(15);

        Optional<String> token = redisLockService.tryLock(lockKey, ttl);
        if (token.isEmpty()) {
            log.debug("Monitor {} is being checked elsewhere, skipping", staleMonitor.getId());
            return;
        }

        try {
            Monitor monitor = monitorRepository.findById(staleMonitor.getId()).orElse(null);
            if (monitor == null || !isStillDue(monitor)) {
                return;   // deleted, or another instance already checked it
            }

            CheckResult result = monitorCheckService.check(monitor);

            MonitorCheck monitorCheck = new MonitorCheck();
            monitorCheck.setMonitor(monitor);
            monitorCheck.setSuccess(result.success());
            monitorCheck.setStatusCode(result.statusCode());
            monitorCheck.setResponseTimeMs(result.responseTimeMs());
            monitorCheck.setFailureReason(result.failureReason());
            monitorCheckRepository.save(monitorCheck);

            applyCheckResult(monitor, result);
            monitor.setLastCheckedAt(LocalDateTime.now());
            monitorRepository.save(monitor);

            log.info("Checked {} ({}): success={}, status={}, monitorStatus={}, time={}ms, reason={}",
                    monitor.getName(), monitor.getUrl(), result.success(), result.statusCode(),
                    monitor.getStatus(), result.responseTimeMs(), result.failureReason());
        } finally {
            redisLockService.unlock(lockKey, token.get());
        }
    }

    private boolean isStillDue(Monitor monitor) {
        return monitor.getLastCheckedAt() == null
                || !monitor.getLastCheckedAt()
                .plusSeconds(monitor.getCheckIntervalSeconds())
                .isAfter(LocalDateTime.now());
    }

    private void applyCheckResult(Monitor monitor, CheckResult result) {
        LocalDateTime now = LocalDateTime.now();

        if (result.success()) {
            monitor.setConsecutiveSuccesses(monitor.getConsecutiveSuccesses() + 1);
            monitor.setConsecutiveFailures(0);
            monitor.setFailureStreakStartedAt(null);

            if (monitor.getConsecutiveSuccesses() == 1) {
                monitor.setSuccessStreakStartedAt(now);
            }

            if (monitor.getConsecutiveSuccesses() >= 2 && monitor.getStatus() != MonitorStatus.UP) {
                log.info("Monitor {} recovered — flipping to UP", monitor.getName());
                monitor.setStatus(MonitorStatus.UP);

                LocalDateTime recoveredAt = monitor.getSuccessStreakStartedAt() != null
                        ? monitor.getSuccessStreakStartedAt() : now;
                incidentRepository.findByMonitorIdAndResolvedFalse(monitor.getId())
                        .ifPresent(incident -> {
                            incident.resolve(recoveredAt);
                            incidentRepository.save(incident);
                        });
            }

            // recovery email: only after 5 continuous minutes of being up
            if (monitor.isAlertActive()
                    && monitor.getStatus() == MonitorStatus.UP
                    && monitor.getSuccessStreakStartedAt() != null
                    && Duration.between(monitor.getSuccessStreakStartedAt(), now)
                    .compareTo(RECOVERY_CONFIRMATION) >= 0) {
                String to = monitorRepository.findOwnerEmail(monitor.getId());
                emailService.sendRecoveryAlert(to, monitor.getName(), monitor.getUrl());
                monitor.setAlertActive(false);
            }
        } else {
            monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
            monitor.setConsecutiveSuccesses(0);
            monitor.setSuccessStreakStartedAt(null);

            if (monitor.getConsecutiveFailures() == 1) {
                monitor.setFailureStreakStartedAt(now);
            }

            if (monitor.getConsecutiveFailures() >= 2 && monitor.getStatus() != MonitorStatus.DOWN) {
                log.warn("Monitor {} failing repeatedly — flipping to DOWN", monitor.getName());
                monitor.setStatus(MonitorStatus.DOWN);

                Incident incident = new Incident();
                incident.setMonitor(monitor);
                incident.setStartedAt(monitor.getFailureStreakStartedAt() != null
                        ? monitor.getFailureStreakStartedAt() : now);
                incident.setFailureReason(result.failureReason());
                incidentRepository.save(incident);

                // DOWN email only if the user isn't already in an alerted state
                if (!monitor.isAlertActive()) {
                    String to = monitorRepository.findOwnerEmail(monitor.getId());
                    emailService.sendDownAlert(to, monitor.getName(), monitor.getUrl(), result.failureReason());
                    monitor.setAlertActive(true);
                    monitor.setLastAlertSentAt(now);
                }
            }

            // reminder every 4 hours while still down
            if (monitor.getStatus() == MonitorStatus.DOWN
                    && monitor.isAlertActive()
                    && monitor.getLastAlertSentAt() != null
                    && Duration.between(monitor.getLastAlertSentAt(), now)
                    .compareTo(REMINDER_INTERVAL) >= 0) {
                String to = monitorRepository.findOwnerEmail(monitor.getId());
                emailService.sendReminder(to, monitor.getName(), monitor.getUrl());
                monitor.setLastAlertSentAt(now);
            }
        }
    }


}