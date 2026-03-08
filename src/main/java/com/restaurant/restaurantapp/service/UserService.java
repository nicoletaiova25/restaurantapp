package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ConflictException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.User;
import com.restaurant.restaurantapp.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        validateId(id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username is required");
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", "username", username);
        }
        return user;
    }

    public User createUser(User user) {
        validatePayload(user, "User payload is required");
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ConflictException("Username already exists: " + user.getUsername());
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        validatePayload(updatedUser, "Updated user payload is required");
        User user = getUserById(id);

        String newUsername = updatedUser.getUsername();
        if (newUsername != null && !newUsername.equals(user.getUsername())
                && userRepository.existsByUsername(newUsername)) {
            throw new ConflictException("Username already exists: " + newUsername);
        }

        user.setUsername(updatedUser.getUsername());
        user.setPassword(updatedUser.getPassword());
        user.setRole(updatedUser.getRole());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        validateId(id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Id must be a positive number");
        }
    }

    private void validatePayload(Object payload, String message) {
        if (payload == null) {
            throw new BadRequestException(message);
        }
    }
}
