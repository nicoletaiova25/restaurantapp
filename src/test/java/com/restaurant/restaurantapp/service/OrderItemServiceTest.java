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
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import com.restaurant.restaurantapp.repository.OrderRepository;
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

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private Order order;
    private MenuItem menuItem;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        order = order(
                restaurantTable(3, 2, user("waiter", "pw", "WAITER")),
                user("waiter", "pw", "WAITER"),
                "OPEN",
                25.0
        );
        order.setId(1L);

        menuItem = menuItem("Soup", 12.5, category("Starters"));
        menuItem.setId(1L);

        orderItem = orderItem(order, menuItem, 2);
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
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem created = orderItemService.createOrderItem(orderItem);

        assertEquals(2, created.getQuantity());
        assertEquals(menuItem, created.getMenuItem());
        assertEquals(order, created.getOrder());
        verify(orderItemRepository).save(orderItem);
        verify(orderRepository).save(order);
    }

    @Test
    void createOrderItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(null));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void updateOrderItemAppliesChanges() {
        Order newOrder = order(
                restaurantTable(5, 4, user("new", "pw", "WAITER")),
                user("new", "pw", "WAITER"),
                "OPEN",
                50.0
        );
        newOrder.setId(2L);

        MenuItem newMenuItem = menuItem("Salad", 9.0, category("Mains"));
        newMenuItem.setId(2L);

        OrderItem updatedItem = orderItem(newOrder, newMenuItem, 3);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(newOrder));
        when(menuItemRepository.findById(2L)).thenReturn(Optional.of(newMenuItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem result = orderItemService.updateOrderItem(1L, updatedItem);

        assertEquals(3, result.getQuantity());
        assertEquals("Salad", result.getMenuItem().getName());
        verify(orderRepository).save(order);
        verify(orderRepository).save(newOrder);
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void updateOrderItemRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> orderItemService.updateOrderItem(1L, null));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItemDeletesWhenPresent() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        orderItemService.deleteOrderItem(1L);

        verify(orderRepository).save(order);
        verify(orderItemRepository).deleteById(1L);
    }

    @Test
    void deleteOrderItemThrowsWhenMissing() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.deleteOrderItem(1L));
        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository, never()).deleteById(anyLong());
    }

    @Test
    void createOrderItemRejectsNullOrder() {
        OrderItem invalidItem = orderItem(null, menuItem, 2);

        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(invalidItem));
    }

    @Test
    void createOrderItemRejectsNullMenuItem() {
        OrderItem invalidItem = orderItem(order, null, 2);

        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(invalidItem));
    }

    @Test
    void createOrderItemRejectsZeroQuantity() {
        OrderItem invalidItem = orderItem(order, menuItem, 0);

        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(invalidItem));
    }

    @Test
    void createOrderItemRejectsNegativeQuantity() {
        OrderItem invalidItem = orderItem(order, menuItem, -5);

        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(invalidItem));
    }

    @Test
    void createOrderItemRejectsClosedOrder() {
        Order closedOrder = order(
                restaurantTable(3, 2, user("waiter", "pw", "WAITER")),
                user("waiter", "pw", "WAITER"),
                "PAID",
                25.0
        );
        closedOrder.setId(1L);

        OrderItem invalidItem = orderItem(closedOrder, menuItem, 2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(closedOrder));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

        assertThrows(BadRequestException.class, () -> orderItemService.createOrderItem(invalidItem));
    }

    @Test
    void createOrderItemUpdatesOrderTotal() {
        order.setTotalPrice(0.0);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderItemService.createOrderItem(orderItem);

        verify(orderRepository).save(order);
        assertEquals(25.0, order.getTotalPrice());
    }

    @Test
    void deleteOrderItemUpdatesOrderTotal() {
        order.setTotalPrice(50.0);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        orderItemService.deleteOrderItem(1L);

        verify(orderRepository).save(order);
        assertEquals(25.0, order.getTotalPrice());
    }

    @Test
    void deleteOrderItemRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> orderItemService.deleteOrderItem(0L));
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void updateOrderItemRejectsNegativeQuantity() {
        OrderItem invalidItem = orderItem(order, menuItem, -1);

        assertThrows(BadRequestException.class, () -> orderItemService.updateOrderItem(1L, invalidItem));
    }

    @Test
    void updateOrderItemRejectsClosedOrder() {
        Order closedOrder = order(
                restaurantTable(3, 2, user("waiter", "pw", "WAITER")),
                user("waiter", "pw", "WAITER"),
                "PAID",
                25.0
        );
        closedOrder.setId(2L);

        MenuItem newMenuItem = menuItem("Salad", 9.0, category("Mains"));
        newMenuItem.setId(2L);

        OrderItem updateItem = orderItem(closedOrder, newMenuItem, 3);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(closedOrder));
        when(menuItemRepository.findById(2L)).thenReturn(Optional.of(newMenuItem));

        assertThrows(BadRequestException.class, () -> orderItemService.updateOrderItem(1L, updateItem));
    }
}