package com.signoz.demo.service;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class AlertEmailService {

    private static final Logger log = LoggerFactory.getLogger(AlertEmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    // Who receives the alert emails
    @Value("${alert.email.to:trupti.thorat38@gmail.com}")
    private String alertRecipient;

    // Who sends the alert emails
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Called by SigNoz webhook — parses the payload
     * and sends a formatted alert email.
     */
    public void processAndSendAlert(Map<String, Object> payload) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("signoz-alert-service");
        Span span = tracer.spanBuilder("processAlert")
                .setAttribute("alert.source", "SigNoz")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            // Extract fields from SigNoz webhook payload
            String alertName    = getField(payload, "alertname", "Unknown Alert");
            String severity     = getField(payload, "severity",  "warning");
            String service      = getField(payload, "service",   "unknown-service");
            String description  = getField(payload, "description", "No description provided");
            String state        = getField(payload, "state",     "firing");
            String currentValue = getField(payload, "value",     "N/A");

            log.warn("Alert received: name={}, severity={}, service={}, traceId={}",
                    alertName, severity, service, traceId);

            span.setAttribute("alert.name", alertName);
            span.setAttribute("alert.severity", severity);
            span.setAttribute("alert.service", service);
            span.setAttribute("alert.state", state);

            // Build and send the email
            String subject = buildSubject(severity, alertName, state);
            String body    = buildEmailBody(
                    alertName, severity, service,
                    description, state, currentValue, traceId
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(alertRecipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            span.setStatus(StatusCode.OK);
            log.info("Alert email sent successfully. TraceId={}", traceId);

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Failed to send alert email: {}", e.getMessage(), e);
        } finally {
            span.end();
        }
    }

    /**
     * Sends a hardcoded test alert — use from Postman
     * to verify email works before connecting SigNoz.
     */
    public void sendTestAlert() {
        Map<String, Object> testPayload = Map.of(
            "alertname",   "High API Error Rate",
            "severity",    "critical",
            "service",     "order-service",
            "description", "Error rate exceeded 10% in the last 5 minutes. " +
                           "Endpoint /orders/place is returning 500 errors.",
            "state",       "firing",
            "value",       "12.5%"
        );
        processAndSendAlert(testPayload);
    }

    // ── Private helpers ──────────────────────────────────────

    private String buildSubject(String severity, String alertName, String state) {
        String emoji = severity.equalsIgnoreCase("critical") ? "[CRITICAL]" :
                       severity.equalsIgnoreCase("warning")  ? "[WARNING]"  : "[INFO]";
        String status = state.equalsIgnoreCase("resolved") ? "RESOLVED" : "FIRING";
        return emoji + " " + status + " - " + alertName;
    }

    private String buildEmailBody(
            String alertName, String severity, String service,
            String description, String state, String value, String traceId) {

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String divider = "=".repeat(50);

        return  divider + "\n" +
                "   SIGNOZ ALERT NOTIFICATION\n" +
                divider + "\n\n" +
                "Alert Name   : " + alertName   + "\n" +
                "Severity     : " + severity.toUpperCase() + "\n" +
                "State        : " + state.toUpperCase()    + "\n" +
                "Service      : " + service      + "\n" +
                "Current Value: " + value         + "\n" +
                "Triggered At : " + time          + "\n" +
                "Trace ID     : " + traceId       + "\n\n" +
                divider + "\n" +
                "DESCRIPTION\n" +
                divider + "\n" +
                description + "\n\n" +
                divider + "\n" +
                "TROUBLESHOOTING STEPS\n" +
                divider + "\n" +
                getTroubleshootingSteps(alertName, severity) + "\n\n" +
                divider + "\n" +
                "View full trace in SigNoz:\n" +
                "http://localhost:3301/traces?traceId=" + traceId + "\n\n" +
                "This is an automated alert from SigNoz via SignozDemo.\n" +
                divider;
    }

    private String getTroubleshootingSteps(String alertName, String severity) {
        // Returns relevant steps based on alert type
        String name = alertName.toLowerCase();

        if (name.contains("error rate") || name.contains("5xx")) {
            return "1. Check SigNoz Traces for recent ERROR spans\n" +
                   "2. Filter by the failing service in SigNoz UI\n" +
                   "3. Look for exception details in span events\n" +
                   "4. Check application logs for stack traces\n" +
                   "5. Verify database connectivity\n" +
                   "6. Check if downstream services are healthy";

        } else if (name.contains("latency") || name.contains("slow")) {
            return "1. Check SigNoz Metrics for p99 latency trend\n" +
                   "2. Look for slow database queries in traces\n" +
                   "3. Check CPU and memory usage\n" +
                   "4. Look for N+1 query problems in JPA logs\n" +
                   "5. Check connection pool exhaustion\n" +
                   "6. Review recent deployments for regressions";

        } else if (name.contains("memory") || name.contains("heap")) {
            return "1. Check JVM heap usage in SigNoz Metrics\n" +
                   "2. Look for memory leak patterns in traces\n" +
                   "3. Trigger a heap dump for analysis\n" +
                   "4. Review recent code changes for leaks\n" +
                   "5. Consider increasing heap size temporarily\n" +
                   "6. Check for unclosed resources in code";

        } else if (name.contains("down") || name.contains("unavailable")) {
            return "1. Check if the service process is running\n" +
                   "2. Verify port is listening: netstat -an | grep 8082\n" +
                   "3. Check Docker container status if containerized\n" +
                   "4. Review startup logs for crash reason\n" +
                   "5. Check disk space and system resources\n" +
                   "6. Restart service if necessary";

        } else {
            return "1. Open SigNoz UI: http://localhost:3301\n" +
                   "2. Go to Alerts section to see alert details\n" +
                   "3. Check Traces filtered by the affected service\n" +
                   "4. Review Metrics dashboard for anomalies\n" +
                   "5. Check application and system logs\n" +
                   "6. Escalate to on-call engineer if unresolved";
        }
    }

    private String getField(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString() : defaultVal;
    }
}