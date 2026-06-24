package com.signoz.demo.model;


import jakarta.persistence.*;

@Entity
@Table(name = "email_logs")
public class EmailRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String toEmail;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String status; // SENT, FAILED
    private String traceId;

    public EmailRequest() {}

    public EmailRequest(String toEmail, String subject, String body) {
        this.toEmail = toEmail;
        this.subject = subject;
        this.body = body;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
