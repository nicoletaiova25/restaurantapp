package com.restaurant.restaurantapp.controller;

import com.restaurant.restaurantapp.model.User;
import com.restaurant.restaurantapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userService.getUserByUsername(username);

        if (user == null || !user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.get("role");

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password is required");
        }

        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body("Role is required");
        }

        role = role.toUpperCase();

        if (!role.equals("MANAGER") && !role.equals("WAITER") && !role.equals("BARTENDER")) {
            return ResponseEntity.badRequest().body("Invalid role");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);

        User savedUser = userService.createUser(user);

        return ResponseEntity.ok(Map.of(
                "id", savedUser.getId(),
                "username", savedUser.getUsername(),
                "role", savedUser.getRole()
        ));
    }
}