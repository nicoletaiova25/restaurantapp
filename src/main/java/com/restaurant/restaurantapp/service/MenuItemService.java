package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public MenuItem getMenuItemById(Long id) {
        validateId(id);
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", "id", id));
    }

    public MenuItem createMenuItem(MenuItem menuItem) {
        validatePayload(menuItem, "Menu item payload is required");
        return menuItemRepository.save(menuItem);
    }

    public MenuItem updateMenuItem(Long id, MenuItem updatedMenuItem) {
        validatePayload(updatedMenuItem, "Updated menu item payload is required");
        MenuItem menuItem = getMenuItemById(id);
        menuItem.setName(updatedMenuItem.getName());
        menuItem.setPrice(updatedMenuItem.getPrice());
        menuItem.setCategory(updatedMenuItem.getCategory());
        return menuItemRepository.save(menuItem);
    }

    public void deleteMenuItem(Long id) {
        validateId(id);
        if (!menuItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Menu item", "id", id);
        }
        menuItemRepository.deleteById(id);
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
