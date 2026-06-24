package com.signoz.demo.controller;

import com.signoz.demo.service.AlertEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertEmailService alertEmailService;

    /**
     * SigNoz calls this webhook when an alert fires.
     * Configure this URL in SigNoz Alert Channel settings.
     */
    @PostMapping("/notify")
    public ResponseEntity<String> receiveAlert(@RequestBody Map<String, Object> payload) {
        alertEmailService.processAndSendAlert(payload);
        return ResponseEntity.ok("Alert received and email sent.");
    }

    /**
     * Test this endpoint manually from Postman
     * to simulate what SigNoz would send.
     */
    @PostMapping("/test")
    public ResponseEntity<String> testAlert() {
        alertEmailService.sendTestAlert();
        return ResponseEntity.ok("Test alert email sent.");
    }
}