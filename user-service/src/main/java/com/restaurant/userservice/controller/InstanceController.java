package com.restaurant.userservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InstanceController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/api/instance")
    public Map<String, String> getInstance() {
        return Map.of(
                "service", "user-service",
                "port", port
        );
    }
}