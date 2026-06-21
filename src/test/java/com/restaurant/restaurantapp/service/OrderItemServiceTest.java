package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.category;
import static com.restaurant.restaurantapp.TestFixtures.menuItem;
import static com.restaurant.restaurantapp.TestFixtures.order;
import static com.restaurant.restaurantapp.TestFixtures.orderItem;
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
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderItem = orderItem(
                order(restaurantTable(3, 2, user("waiter", "pw", "WAITER")), user("waiter", "pw", "WAITER"), "OPEN", 25.0),
                menuItem("Soup", 12.5, category("Starters")),
                2);
        orderItem.setId(1L);
    }

    @Test
    void getAllOrderItemsReturnsRepositoryResults() {
        when(orderItemRepository.findAll()).thenReturn(List.of(orderItem));

        List<OrderItem> items = orderItemService.getAllOrderItems();

        assertEquals(1, items.size());
        verify(orderItemRepository).findAll();
    }

    @Test
    void getOrderItemByIdReturnsOrderItemWhenFound() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        OrderItem result = orderItemService.getOrderItemById(1L);

        assertEquals(orderItem, result);
        verify(orderItemRepository).findById(1L);
    }

    @Test
    void getOrderItemByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> orderItemService.getOrderItemById(0L));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void getOrderItemByIdThrowsWhenMissing() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.getOrderItemById(99L));
        verify(orderItemRepository).findById(99L);
    }

    @Test
    void createOrderItemSavesPayload() {
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem created = orderItemService.createOrderItem(orderItem);

        assertEquals(2, created.getQuantity());
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void createOrderItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(null));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void updateOrderItemAppliesChanges() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem result = orderItemService.updateOrderItem(1L,
                orderItem(order(restaurantTable(5, 4, user("new", "pw", "WAITER")), user("new", "pw", "WAITER"), "PAID", 50.0),
                        menuItem("Salad", 9.0, category("Mains")),
                        3));

        assertEquals(3, result.getQuantity());
        assertEquals("Salad", result.getMenuItem().getName());
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void updateOrderItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderItemService.updateOrderItem(1L, null));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItemDeletesWhenPresent() {
        when(orderItemRepository.existsById(1L)).thenReturn(true);

        orderItemService.deleteOrderItem(1L);

        verify(orderItemRepository).deleteById(1L);
    }

    @Test
    void deleteOrderItemThrowsWhenMissing() {
        when(orderItemRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.deleteOrderItem(1L));
        verify(orderItemRepository).existsById(1L);
        verify(orderItemRepository, never()).deleteById(anyLong());
    }
}

