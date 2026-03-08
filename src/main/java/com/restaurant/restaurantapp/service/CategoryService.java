package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Category;
import com.restaurant.restaurantapp.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        validateId(id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    public Category createCategory(Category category) {
        validatePayload(category, "Category payload is required");
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category updatedCategory) {
        validatePayload(updatedCategory, "Updated category payload is required");
        Category category = getCategoryById(id);
        category.setName(updatedCategory.getName());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        validateId(id);
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        categoryRepository.deleteById(id);
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
