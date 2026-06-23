package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.category;
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
import com.restaurant.restaurantapp.model.Category;
import com.restaurant.restaurantapp.repository.CategoryRepository;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = category("Starters");
        category.setId(1L);
    }

    @Test
    void getAllCategoriesReturnsRepositoryResults() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<Category> categories = categoryService.getAllCategories();

        assertEquals(1, categories.size());
        verify(categoryRepository).findAll();
    }

    @Test
    void getCategoryByIdReturnsCategoryWhenFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(1L);

        assertEquals(category, result);
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getCategoryByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> categoryService.getCategoryById(0L));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void getCategoryByIdThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L));
        verify(categoryRepository).findById(99L);
    }

    @Test
    void createCategorySavesPayload() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.createCategory(category("Desserts"));

        assertEquals("Desserts", created.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategoryRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> categoryService.createCategory(null));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateCategoryAppliesChanges() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.updateCategory(1L, category("Mains"));

        assertEquals("Mains", result.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategoryRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> categoryService.updateCategory(1L, null));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void deleteCategoryDeletesWhenPresent() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategoryThrowsWhenMissing() {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(1L));
        verify(categoryRepository).existsById(1L);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void getCategoryByIdRejectsNullId() {
        assertThrows(BadRequestException.class, () -> categoryService.getCategoryById(null));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateCategoryRejectsNullId() {
        assertThrows(BadRequestException.class, () -> categoryService.updateCategory(-1L, category("Updated")));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void deleteCategoryRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(0L));
        verifyNoInteractions(categoryRepository);
    }
}

