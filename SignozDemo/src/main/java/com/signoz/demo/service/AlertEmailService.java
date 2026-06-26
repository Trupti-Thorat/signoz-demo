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

    public void processAndSendAlert(Map<String, Object> payload) {
        String alertName = String.valueOf(payload.getOrDefault("alertname", "Unknown Alert"));
        String severity  = String.valueOf(payload.getOrDefault("severity",  "unknown"));
        String message   = String.valueOf(payload.getOrDefault("message",   "No message provided"));
        String state     = String.valueOf(payload.getOrDefault("state",     "firing"));

        String subject = "[SigNoz Alert] " + alertName + " - " + severity.toUpperCase();
        String body    = "Alert Details:\n\n"
                       + "Alert Name : " + alertName + "\n"
                       + "Severity   : " + severity  + "\n"
                       + "State      : " + state     + "\n"
                       + "Message    : " + message   + "\n"
                       + "Payload    : " + payload.toString();

        sendEmail(subject, body);
    }

    public void sendTestAlert() {
        sendEmail(
            "[SigNoz Test] Alert system working",
            "This is a test alert from SignozDemo.\nYour webhook and email setup is working correctly."
        );
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