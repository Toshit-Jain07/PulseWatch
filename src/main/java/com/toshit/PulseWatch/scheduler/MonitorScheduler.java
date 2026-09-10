package com.toshit.PulseWatch.scheduler;

import com.toshit.PulseWatch.checker.CheckResult;
import com.toshit.PulseWatch.checker.MonitorCheckService;
import com.toshit.PulseWatch.entity.Monitor;
import com.toshit.PulseWatch.entity.MonitorCheck;
import com.toshit.PulseWatch.entity.MonitorStatus;
import com.toshit.PulseWatch.repository.MonitorCheckRepository;
import com.toshit.PulseWatch.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckService monitorCheckService;
    private final MonitorCheckRepository monitorCheckRepository;

    @Scheduled(fixedRate = 10000)
    public void checkDueMonitors() {
        List<Monitor> dueMonitors = monitorRepository.findDueMonitors(LocalDateTime.now());
        log.info("Scheduler tick: {} monitor(s) due for a check", dueMonitors.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Monitor monitor : dueMonitors) {
                executor.submit(() -> {
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

                    log.info("Checked {} ({}): success={}, status={}, time={}ms, reason={}",
                            monitor.getName(),
                            monitor.getUrl(),
                            result.success(),
                            result.statusCode(),
                            result.responseTimeMs(),
                            result.failureReason());
                });            }
        } // executor.close() is called automatically here — waits for all submitted tasks to finish
    }

    private void applyCheckResult(Monitor monitor, CheckResult result) {
        if (result.success()) {
            monitor.setConsecutiveSuccesses(monitor.getConsecutiveSuccesses() + 1);
            monitor.setConsecutiveFailures(0);

            if (monitor.getConsecutiveSuccesses() >= 2 && monitor.getStatus() != MonitorStatus.UP) {
                log.info("Monitor {} recovered — flipping to UP", monitor.getName());
                monitor.setStatus(MonitorStatus.UP);
            }
        } else {
            monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
            monitor.setConsecutiveSuccesses(0);

            if (monitor.getConsecutiveFailures() >= 2 && monitor.getStatus() != MonitorStatus.DOWN) {
                log.warn("Monitor {} failing repeatedly — flipping to DOWN", monitor.getName());
                monitor.setStatus(MonitorStatus.DOWN);
            }
        }
    }


}