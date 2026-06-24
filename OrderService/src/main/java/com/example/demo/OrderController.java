package com.example.demo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Order Service!";
    }

    /**
     * Place an order - creates a traced span and logs the event.
     * 
     * curl -X POST http://localhost:8082/orders/place \
     *   -H "Content-Type: application/json" \
     *   -d '{"item":"Laptop","quantity":1,"price":999.99}'
     */
    @PostMapping("/place")
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody Map<String, Object> orderDetails) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("order-service");
        Span span = tracer.spanBuilder("placeOrder")
                .setAttribute("order.item", String.valueOf(orderDetails.getOrDefault("item", "unknown")))
                .setAttribute("order.quantity", String.valueOf(orderDetails.getOrDefault("quantity", 0)))
                .startSpan();

        String traceId = span.getSpanContext().getTraceId();
        log.info("Placing order. TraceId={}, Details={}", traceId, orderDetails);

        try (Scope scope = span.makeCurrent()) {
            // Simulate processing time
            Thread.sleep(50);

            span.setStatus(StatusCode.OK);
            log.info("Order placed successfully. TraceId={}", traceId);

            return ResponseEntity.ok(Map.of(
                    "status", "ORDER_PLACED",
                    "traceId", traceId,
                    "message", "Order placed successfully for: " + orderDetails.getOrDefault("item", "unknown")
            ));
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Order failed. TraceId={}", traceId, e);
            return ResponseEntity.internalServerError().body(Map.of("status", "FAILED", "error", e.getMessage()));
        } finally {
            span.end();
        }
    }
}