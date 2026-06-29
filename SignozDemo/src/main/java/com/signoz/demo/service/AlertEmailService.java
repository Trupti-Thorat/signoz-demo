package com.signoz.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class AlertEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${alert.email.to}")
    private String alertEmailTo;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @SuppressWarnings("unchecked")
    public void processAndSendAlert(Map<String, Object> payload) {
        String alertName = getStr(payload, "alertname", "Unknown Alert");
        String severity  = getStr(payload, "severity",  "critical");
        String state     = getStr(payload, "state",     "firing");
        String message   = getStr(payload, "message",   "No message provided");

        // Safely extract service name from nested labels map
        String service = "Unknown Service";
        Object labelsObj = payload.get("labels");
        if (labelsObj instanceof Map) {
            Map<String, Object> labels = (Map<String, Object>) labelsObj;
            service = getStr(labels, "service_name", "Unknown Service");
        }

        String subject = "[ALERT] " + service + " - " + alertName;
        String body = "Dear Manager,\n\n"
            + "An alert has been triggered!\n\n"
            + "Service   : " + service   + "\n"
            + "Alert     : " + alertName + "\n"
            + "Severity  : " + severity  + "\n"
            + "State     : " + state     + "\n"
            + "Message   : " + message   + "\n"
            + "Time      : " + java.time.LocalDateTime.now() + "\n\n"
            + "Check SigNoz: http://localhost:3301\n\n"
            + "Regards,\nSigNoz Monitoring";

        sendEmail(subject, body);
    }

    public void sendTestAlert() {
        sendEmail(
            "[SigNoz Test] Alert system working",
            "This is a test alert from SignozDemo.\n"
            + "Your webhook and email setup is working correctly."
        );
    }

    // ── helper: safely get a String from any Map<String, Object> ──
    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : defaultVal;
    }

    private void sendEmail(String subject, String body) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(alertEmailTo);
        mail.setSubject(subject);
        mail.setText(body);
        mailSender.send(mail);
    }
}