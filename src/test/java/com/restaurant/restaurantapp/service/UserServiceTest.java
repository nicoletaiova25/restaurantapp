package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ConflictException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.User;
import com.restaurant.restaurantapp.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = user("admin", "secret", "ADMIN");
        existingUser.setId(1L);
    }

    @Test
    void getAllUsersReturnsRepositoryResults() {
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("admin", users.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void getUserByIdReturnsUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.getUserById(1L);

        assertEquals(existingUser, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> userService.getUserById(0L));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
        verify(userRepository).findById(99L);
    }

    @Test
    void getUserByUsernameReturnsUserWhenFound() {
        when(userRepository.findByUsername("admin")).thenReturn(existingUser);

        User result = userService.getUserByUsername("admin");

        assertEquals(existingUser, result);
        verify(userRepository).findByUsername("admin");
    }

    @Test
    void getUserByUsernameRejectsBlankUsername() {
        assertThrows(BadRequestException.class, () -> userService.getUserByUsername("  "));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUsername("missing"));
        verify(userRepository).findByUsername("missing");
    }

    @Test
    void createUserSavesWhenUsernameIsUnique() {
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(user("new-user", "pw", "WAITER"));

        assertEquals("new-user", created.getUsername());
        verify(userRepository).existsByUsername("new-user");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(user("admin", "pw", "WAITER")));
        verify(userRepository).existsByUsername("admin");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> userService.createUser(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserAppliesChangesWhenUsernameStaysUnique() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, user("editor", "new-secret", "MANAGER"));

        assertEquals("editor", result.getUsername());
        assertEquals("new-secret", result.getPassword());
        assertEquals("MANAGER", result.getRole());
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUserRejectsDuplicateUsername() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("other" )).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.updateUser(1L, user("other", "pw", "ADMIN")));
        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("other");
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserDeletesWhenPresent() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserThrowsWhenMissing() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
