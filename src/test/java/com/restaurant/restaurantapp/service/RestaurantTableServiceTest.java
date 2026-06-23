package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.restaurantTable;
import static com.restaurant.restaurantapp.TestFixtures.user;
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
import com.restaurant.restaurantapp.model.RestaurantTable;
import com.restaurant.restaurantapp.repository.RestaurantTableRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = restaurantTable(7, 4, user("waiter", "pw", "WAITER"));
        table.setId(1L);
    }

    @Test
    void getAllTablesReturnsRepositoryResults() {
        when(restaurantTableRepository.findAll()).thenReturn(List.of(table));

        List<RestaurantTable> tables = restaurantTableService.getAllTables();

        assertEquals(1, tables.size());
        verify(restaurantTableRepository).findAll();
    }

    @Test
    void getTableByIdReturnsTableWhenFound() {
        when(restaurantTableRepository.findById(1L)).thenReturn(Optional.of(table));

        RestaurantTable result = restaurantTableService.getTableById(1L);

        assertEquals(table, result);
        verify(restaurantTableRepository).findById(1L);
    }

    @Test
    void getTableByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> restaurantTableService.getTableById(null));
        verifyNoInteractions(restaurantTableRepository);
    }

    @Test
    void getTableByIdThrowsWhenMissing() {
        when(restaurantTableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantTableService.getTableById(99L));
        verify(restaurantTableRepository).findById(99L);
    }

    @Test
    void createTableSavesPayload() {
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantTable created = restaurantTableService.createTable(restaurantTable(12, 2, user("w", "pw", "WAITER")));

        assertEquals(12, created.getTableNumber());
        verify(restaurantTableRepository).save(any(RestaurantTable.class));
    }

    @Test
    void createTableRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> restaurantTableService.createTable(null));
        verifyNoInteractions(restaurantTableRepository);
    }

    @Test
    void updateTableAppliesChanges() {
        when(restaurantTableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantTable result = restaurantTableService.updateTable(1L, restaurantTable(9, 6, user("new", "pw", "WAITER")));

        assertEquals(9, result.getTableNumber());
        assertEquals(6, result.getSeats());
        assertEquals("new", result.getWaiter().getUsername());
        verify(restaurantTableRepository).save(table);
    }

    @Test
    void updateTableRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> restaurantTableService.updateTable(1L, null));
        verifyNoInteractions(restaurantTableRepository);
    }

    @Test
    void deleteTableDeletesWhenPresent() {
        when(restaurantTableRepository.existsById(1L)).thenReturn(true);

        restaurantTableService.deleteTable(1L);

        verify(restaurantTableRepository).deleteById(1L);
    }

    @Test
    void deleteTableThrowsWhenMissing() {
        when(restaurantTableRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> restaurantTableService.deleteTable(1L));
        verify(restaurantTableRepository).existsById(1L);
        verify(restaurantTableRepository, never()).deleteById(anyLong());
    }

    @Test
    void getTableByIdRejectsZeroId() {
        assertThrows(BadRequestException.class, () -> restaurantTableService.getTableById(0L));
        verifyNoInteractions(restaurantTableRepository);
    }

    @Test
    void deleteTableRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> restaurantTableService.deleteTable(-1L));
        verifyNoInteractions(restaurantTableRepository);
    }
}

