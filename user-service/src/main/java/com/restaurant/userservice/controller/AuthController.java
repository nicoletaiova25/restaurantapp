package com.restaurant.userservice.controller;

import com.restaurant.userservice.model.User;
import com.restaurant.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            User user = userService.getUserByUsername(body.get("username"));

            if (!user.getPassword().equals(body.get("password"))) {
                return ResponseEntity.status(401).body("Invalid username or password");
            }

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        User user = new User();
        user.setUsername(body.get("username"));
        user.setPassword(body.get("password"));
        user.setRole(body.get("role"));

        try {
            User savedUser = userService.createUser(user);

            return ResponseEntity.ok(Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "role", savedUser.getRole()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(409).body("Username already exists");
        }
    }
}