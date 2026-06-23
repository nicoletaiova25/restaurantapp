package com.restaurant.reportingservice.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportingController {

    @PersistenceContext
    private EntityManager entityManager;

    private final RestTemplate restTemplate;

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Long usersCount = ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM app_user")
                .getSingleResult()).longValue();

        Long ordersCount = ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM orders")
                .getSingleResult()).longValue();

        Double totalSales = ((Number) entityManager
                .createNativeQuery("SELECT COALESCE(SUM(total_price), 0) FROM orders")
                .getSingleResult()).doubleValue();

        Long paidOrders = ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM orders WHERE status = 'PAID'")
                .getSingleResult()).longValue();

        return Map.of(
                "usersCount", usersCount,
                "ordersCount", ordersCount,
                "paidOrders", paidOrders,
                "totalSales", totalSales
        );
    }

    public ReportingController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/users-from-user-service")
    public Object getUsersFromUserService() {
        return restTemplate.getForObject(
                "http://USER-SERVICE/api/users",
                Object.class
        );
    }

    @GetMapping("/user-instance")
    public Object getUserServiceInstance() {
        return restTemplate.getForObject(
                "http://USER-SERVICE/api/instance",
                Object.class
        );
    }

    @GetMapping("/safe-users")
    @CircuitBreaker(name = "userService", fallbackMethod = "userServiceFallback")
    @Retry(name = "userService")
    public Object getSafeUsers() {
        return restTemplate.getForObject(
                "http://USER-SERVICE/api/users",
                Object.class
        );
    }

    public Object userServiceFallback(Throwable ex) {
        return Map.of(
                "message", "User Service is temporarily unavailable",
                "fallback", true
        );
    }

    @GetMapping("/safe-categories")
    @CircuitBreaker(name = "restaurantService", fallbackMethod = "restaurantServiceFallback")
    @Retry(name = "restaurantService")
    public Object getSafeCategories() {
        return restTemplate.getForObject(
                "http://RESTAURANTAPP/api/categories",
                Object.class
        );
    }

    public Object restaurantServiceFallback(Throwable ex) {
        return Map.of(
                "message", "Restaurant Service is temporarily unavailable",
                "fallback", true
        );
    }
}