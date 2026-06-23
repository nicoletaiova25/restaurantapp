package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.order;
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
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.model.RestaurantTable;
import com.restaurant.restaurantapp.model.User;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order existingOrder;

    @BeforeEach
    void setUp() {
        existingOrder = order(
                restaurantTable(5, 4, user("waiter", "pw", "WAITER")),
                user("waiter", "pw", "WAITER"),
                "OPEN",
                42.0);
        existingOrder.setId(1L);
    }

    @Test
    void getAllOrdersReturnsRepositoryResults() {
        when(orderRepository.findAll()).thenReturn(List.of(existingOrder));

        List<Order> orders = orderService.getAllOrders();

        assertEquals(1, orders.size());
        verify(orderRepository).findAll();
    }

    @Test
    void getOrderByIdReturnsOrderWhenFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        Order result = orderService.getOrderById(1L);

        assertEquals(existingOrder, result);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> orderService.getOrderById(null));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderByIdThrowsWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository).findById(99L);
    }

    @Test
    void createOrderRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderService.createOrder(null));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void updateOrderAppliesChanges() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrder(1L,
                order(restaurantTable(6, 2, user("new", "pw", "WAITER")), user("new", "pw", "WAITER"), "CLOSED", 55.5));

        assertEquals("CLOSED", result.getStatus());
        assertEquals(6, result.getTable().getTableNumber());
        verify(orderRepository).save(existingOrder);
    }

    @Test
    void updateOrderRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderService.updateOrder(1L, null));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void deleteOrderDeletesWhenPresent() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrderThrowsWhenMissing() {
        when(orderRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(1L));
        verify(orderRepository).existsById(1L);
        verify(orderRepository, never()).deleteById(anyLong());
    }

    @Test
    void createOrderRejectsNullTable() {
        Order invalidOrder = new Order();
        invalidOrder.setTable(null);
        invalidOrder.setWaiter(user("waiter", "pw", "WAITER"));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(invalidOrder));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsNullWaiter() {
        Order invalidOrder = new Order();
        invalidOrder.setTable(restaurantTable(5, 4, user("waiter", "pw", "WAITER")));
        invalidOrder.setWaiter(null);

        assertThrows(BadRequestException.class, () -> orderService.createOrder(invalidOrder));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderPreservesStatusIfNull() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        Order updateOrder = order(restaurantTable(6, 2, user("new", "pw", "WAITER")), user("new", "pw", "WAITER"), null, 55.5);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrder(1L, updateOrder);

        assertEquals("OPEN", result.getStatus()); // Original status preserved
    }

    @Test
    void updateOrderPreservesStatusIfBlank() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        Order updateOrder = order(restaurantTable(6, 2, user("new", "pw", "WAITER")), user("new", "pw", "WAITER"), "  ", 55.5);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrder(1L, updateOrder);

        assertEquals("OPEN", result.getStatus()); // Original status preserved
    }

    @Test
    void getOrderByIdRejectsNullId() {
        assertThrows(BadRequestException.class, () -> orderService.getOrderById(null));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderByIdRejectsZeroId() {
        assertThrows(BadRequestException.class, () -> orderService.getOrderById(0L));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void deleteOrderRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> orderService.deleteOrder(-1L));
        verifyNoInteractions(orderRepository);
    }
}

