package com.signoz.demo.scheduler;

import com.signoz.demo.service.AlertEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    @Autowired
    private AlertEmailService alertEmailService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Boolean> serviceStatus = new ConcurrentHashMap<>();
    private final Map<String, Boolean> slowStatus    = new ConcurrentHashMap<>();

    // Response time threshold in milliseconds
    private static final long SLOW_THRESHOLD_MS = 1000;

    // ── Health checks every 60 seconds ──────────────────────────
    @Scheduled(fixedDelay = 60000)
    public void checkServices() {
        log.info("Running health checks...");
        checkService("order-service",   "http://order-service:8082/actuator/health");
        checkService("payment-service", "http://payment-service:8083/actuator/health");
    }

    // ── Response time checks every 30 seconds ────────────────────
    @Scheduled(fixedDelay = 30000)
    public void checkResponseTimes() {
        log.info("Running response time checks...");
        checkResponseTime("order-service",   "http://order-service:8082/orders/hello");
        checkResponseTime("payment-service", "http://payment-service:8083/payment/hello");
    }

    // ── Health check logic ───────────────────────────────────────
    private void checkService(String name, String url) {
        Boolean previouslyUp = serviceStatus.get(name);
        try {
            String response = restTemplate.getForObject(url, String.class);
            boolean isUp = response != null && response.contains("UP");

            if (isUp) {
                log.info("{} is UP", name);
                if (Boolean.FALSE.equals(previouslyUp)) {
                    log.info("{} RECOVERED — sending recovery email", name);
                    alertEmailService.processAndSendAlert(Map.of(
                        "alertname", "ServiceRecovered",
                        "state",     "resolved",
                        "severity",  "info",
                        "labels",    Map.of("service_name", name),
                        "message",   name + " is back UP and healthy"
                    ));
                }
                serviceStatus.put(name, true);
            } else {
                if (!Boolean.FALSE.equals(previouslyUp)) {
                    alertEmailService.processAndSendAlert(Map.of(
                        "alertname", "ServiceDegraded",
                        "state",     "firing",
                        "severity",  "warning",
                        "labels",    Map.of("service_name", name),
                        "message",   name + " returned unexpected health response"
                    ));
                }
                serviceStatus.put(name, false);
            }
        } catch (Exception e) {
            log.error("{} is DOWN: {}", name, e.getMessage());
            if (!Boolean.FALSE.equals(previouslyUp)) {
                alertEmailService.processAndSendAlert(Map.of(
                    "alertname", "ServiceDown",
                    "state",     "firing",
                    "severity",  "critical",
                    "labels",    Map.of("service_name", name),
                    "message",   name + " is DOWN: " + e.getMessage()
                ));
            }
            serviceStatus.put(name, false);
        }
    }

    // ── Response time check logic ────────────────────────────────
    private void checkResponseTime(String name, String url) {
        Boolean previouslySlow = slowStatus.get(name);
        try {
            long start    = System.currentTimeMillis();
            restTemplate.getForObject(url, String.class);
            long duration = System.currentTimeMillis() - start;

            log.info("{} response time: {}ms", name, duration);

            if (duration > SLOW_THRESHOLD_MS) {
                log.warn("{} is SLOW: {}ms (threshold: {}ms)", name, duration, SLOW_THRESHOLD_MS);

                // Only email on first detection, not every 30s
                if (!Boolean.TRUE.equals(previouslySlow)) {
                    alertEmailService.processAndSendAlert(Map.of(
                        "alertname", "HighResponseTime",
                        "state",     "firing",
                        "severity",  "warning",
                        "labels",    Map.of("service_name", name),
                        "message",   name + " response time is " + duration
                                     + "ms (threshold: " + SLOW_THRESHOLD_MS + "ms)"
                    ));
                }
                slowStatus.put(name, true);

            } else {
                // Was it slow before? → Send recovery email
                if (Boolean.TRUE.equals(previouslySlow)) {
                    log.info("{} response time back to normal: {}ms", name, duration);
                    alertEmailService.processAndSendAlert(Map.of(
                        "alertname", "ResponseTimeNormal",
                        "state",     "resolved",
                        "severity",  "info",
                        "labels",    Map.of("service_name", name),
                        "message",   name + " response time is back to normal: "
                                     + duration + "ms"
                    ));
                }
                slowStatus.put(name, false);
            }

        } catch (Exception e) {
            log.error("Could not check response time for {}: {}", name, e.getMessage());
        }
    }
}