package com.toshit.PulseWatch.checker;

import com.toshit.PulseWatch.entity.Monitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Service
@Slf4j
public class MonitorCheckService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CheckResult check(Monitor monitor) {
        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .timeout(Duration.ofMillis(monitor.getTimeoutMs()))
                    .method(monitor.getHttpMethod().name(), HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long responseTime = System.currentTimeMillis() - startTime;

            boolean statusMatches = response.statusCode() == monitor.getExpectedStatusCode();

            return new CheckResult(
                    statusMatches,
                    response.statusCode(),
                    responseTime,
                    statusMatches ? null : "STATUS_MISMATCH"
            );

        } catch (HttpTimeoutException e) {
            return failure(startTime, "TIMEOUT");
        } catch (ConnectException e) {
            return failure(startTime, "CONNECTION_REFUSED");
        } catch (UnknownHostException e) {
            return failure(startTime, "DNS_FAILURE");
        } catch (Exception e) {
            log.warn("Unexpected error checking monitor {}: {}", monitor.getId(), e.getMessage());
            return failure(startTime, "UNKNOWN_ERROR");
        }
    }

    private CheckResult failure(long startTime, String reason) {
        long responseTime = System.currentTimeMillis() - startTime;
        return new CheckResult(false, null, responseTime, reason);
    }
}