package com.signoz.demo.controller;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.signoz.demo.service.EmailService;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * Send an email via POST request.
     * 
     * Example curl:
     *   curl -X POST http://localhost:8081/api/email/send \
     *     -H "Content-Type: application/json" \
     *     -d '{"to":"you@example.com","subject":"Test","body":"Hello from SigNoz!"}'
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        if (to == null || subject == null || body == null) {
            return ResponseEntity.badRequest().body("Missing required fields: to, subject, body");
        }

        String result = emailService.sendEmail(to, subject, body);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Email service is running!");
    }
}