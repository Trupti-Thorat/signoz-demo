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

    // Tracks previous state of each service: true = UP, false = DOWN
    // ConcurrentHashMap is thread-safe for scheduled tasks
    private final Map<String, Boolean> serviceStatus = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 60000)
    public void checkServices() {
        log.info("Running health checks for all services...");
        checkService("order-service",   "http://order-service:8082/actuator/health");
        checkService("payment-service", "http://payment-service:8083/actuator/health");
    }

    private void checkService(String name, String url) {
        // Get previous state — assume UP on first run (null = never checked)
        Boolean previouslyUp = serviceStatus.get(name);

        try {
            String response = restTemplate.getForObject(url, String.class);
            boolean isUp = response != null && response.contains("UP");

            if (isUp) {
                log.info("{} is UP", name);

                // Was it DOWN before? → Send RECOVERY email
                if (Boolean.FALSE.equals(previouslyUp)) {
                    log.info("{} has RECOVERED — sending recovery email", name);
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
                log.warn("{} returned unexpected response: {}", name, response);

                // Only send alert if it was UP before (avoid repeat emails)
                if (!Boolean.FALSE.equals(previouslyUp)) {
                    alertEmailService.processAndSendAlert(Map.of(
                        "alertname", "ServiceDegraded",
                        "state",     "firing",
                        "severity",  "warning",
                        "labels",    Map.of("service_name", name),
                        "message",   name + " health check returned unexpected response"
                    ));
                }

                serviceStatus.put(name, false);
            }

        } catch (Exception e) {
            log.error("{} is DOWN! Error: {}", name, e.getMessage());

            // Only send DOWN alert if it was UP before (avoid repeat emails)
            if (!Boolean.FALSE.equals(previouslyUp)) {
                log.info("{} just went DOWN — sending alert email", name);
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
}