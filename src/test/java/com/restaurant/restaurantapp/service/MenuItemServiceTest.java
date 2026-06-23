package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.category;
import static com.restaurant.restaurantapp.TestFixtures.menuItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuItemService menuItemService;

    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        menuItem = menuItem("Soup", 12.5, category("Starters"));
        menuItem.setId(1L);
    }

    @Test
    void getAllMenuItemsReturnsRepositoryResults() {
        when(menuItemRepository.findAll()).thenReturn(List.of(menuItem));

        List<MenuItem> menuItems = menuItemService.getAllMenuItems();

        assertEquals(1, menuItems.size());
        verify(menuItemRepository).findAll();
    }

    @Test
    void getMenuItemByIdReturnsMenuItemWhenFound() {
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

        MenuItem result = menuItemService.getMenuItemById(1L);

        assertEquals(menuItem, result);
        verify(menuItemRepository).findById(1L);
    }

    @Test
    void getMenuItemByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> menuItemService.getMenuItemById(-1L));
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void getMenuItemByIdThrowsWhenMissing() {
        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.getMenuItemById(99L));
        verify(menuItemRepository).findById(99L);
    }

    @Test
    void createMenuItemSavesPayload() {
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem created = menuItemService.createMenuItem(menuItem("Cake", 8.0, category("Desserts")));

        assertEquals("Cake", created.getName());
        verify(menuItemRepository).save(any(MenuItem.class));
    }

    @Test
    void createMenuItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> menuItemService.createMenuItem(null));
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void updateMenuItemAppliesChanges() {
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem result = menuItemService.updateMenuItem(1L, menuItem("Salad", 9.0, category("Mains")));

        assertEquals("Salad", result.getName());
        assertEquals(9.0, result.getPrice());
        assertEquals("Mains", result.getCategory().getName());
        verify(menuItemRepository).save(menuItem);
    }

    @Test
    void updateMenuItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> menuItemService.updateMenuItem(1L, null));
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void deleteMenuItemDeletesWhenPresent() {
        when(menuItemRepository.existsById(1L)).thenReturn(true);

        menuItemService.deleteMenuItem(1L);

        verify(menuItemRepository).deleteById(1L);
    }

    @Test
    void deleteMenuItemThrowsWhenMissing() {
        when(menuItemRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.deleteMenuItem(1L));
        verify(menuItemRepository).existsById(1L);
        verify(menuItemRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllMenuItemsPageableReturnsRepositoryResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuItem> expectedPage = new PageImpl<>(List.of(menuItem), pageable, 1);
        when(menuItemRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<MenuItem> result = menuItemService.getAllMenuItems(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Soup", result.getContent().get(0).getName());
        verify(menuItemRepository).findAll(pageable);
    }

    @Test
    void getMenuItemByIdRejectsNullId() {
        assertThrows(BadRequestException.class, () -> menuItemService.getMenuItemById(null));
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void deleteMenuItemRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> menuItemService.deleteMenuItem(0L));
        verifyNoInteractions(menuItemRepository);
    }
}

