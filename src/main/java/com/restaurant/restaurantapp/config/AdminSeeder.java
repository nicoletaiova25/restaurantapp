package com.restaurant.restaurantapp.config;

import com.restaurant.restaurantapp.model.User;
import com.restaurant.restaurantapp.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserService userService;

    public AdminSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        try {
            userService.getUserByUsername("admin");
        } catch (Exception ex) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin");
            admin.setRole("ADMIN");

            userService.createUser(admin);

            System.out.println("Default admin created: admin / admin");
        }
    }
}