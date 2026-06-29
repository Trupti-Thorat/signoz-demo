package com.signoz.demo.scheduler;


import com.signoz.demo.service.AlertEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    @Autowired
    private AlertEmailService alertEmailService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 60000)  // runs every 60 seconds
    public void checkServices() {
        log.info("Running health checks for all services...");
        checkService("order-service",   "http://order-service:8082/actuator/health");
        checkService("payment-service", "http://payment-service:8083/actuator/health");
    }

    private void checkService(String name, String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && response.contains("UP")) {
                log.info("{} is UP", name);
            } else {
                log.warn("{} returned unexpected response: {}", name, response);
                alertEmailService.processAndSendAlert(Map.of(
                    "alertname", "ServiceDegraded",
                    "state",     "firing",
                    "severity",  "warning",
                    "labels",    Map.of("service_name", name),
                    "message",   name + " health check returned unexpected response"
                ));
            }
        } catch (Exception e) {
            log.error("{} is DOWN! Error: {}", name, e.getMessage());
            alertEmailService.processAndSendAlert(Map.of(
                "alertname", "ServiceDown",
                "state",     "firing",
                "severity",  "critical",
                "labels",    Map.of("service_name", name),
                "message",   name + " is DOWN: " + e.getMessage()
            ));
        }
    }
}