package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import com.restaurant.restaurantapp.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderItemService(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            MenuItemRepository menuItemRepository
    ) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    public OrderItem getOrderItemById(Long id) {
        validateId(id);
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item", "id", id));
    }

    public OrderItem createOrderItem(OrderItem orderItem) {
        validatePayload(orderItem, "Order item payload is required");
        validateOrderItem(orderItem);

        Order order = orderRepository.findById(orderItem.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderItem.getOrder().getId()));

        MenuItem menuItem = menuItemRepository.findById(orderItem.getMenuItem().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", "id", orderItem.getMenuItem().getId()));

        if (!"OPEN".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("You can add items only to an OPEN order");
        }

        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);

        double itemTotal = menuItem.getPrice() * orderItem.getQuantity();

        OrderItem savedItem = orderItemRepository.save(orderItem);

        order.setTotalPrice(order.getTotalPrice() + itemTotal);
        orderRepository.save(order);

        return savedItem;
    }

    public OrderItem updateOrderItem(Long id, OrderItem updatedOrderItem) {
        validatePayload(updatedOrderItem, "Updated order item payload is required");
        validateOrderItem(updatedOrderItem);

        OrderItem oldItem = getOrderItemById(id);

        Order oldOrder = oldItem.getOrder();
        double oldTotal = oldItem.getMenuItem().getPrice() * oldItem.getQuantity();

        oldOrder.setTotalPrice(Math.max(0, oldOrder.getTotalPrice() - oldTotal));
        orderRepository.save(oldOrder);

        Order newOrder = orderRepository.findById(updatedOrderItem.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", updatedOrderItem.getOrder().getId()));

        MenuItem newMenuItem = menuItemRepository.findById(updatedOrderItem.getMenuItem().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", "id", updatedOrderItem.getMenuItem().getId()));

        if (!"OPEN".equalsIgnoreCase(newOrder.getStatus())) {
            throw new BadRequestException("You can update items only for an OPEN order");
        }

        oldItem.setOrder(newOrder);
        oldItem.setMenuItem(newMenuItem);
        oldItem.setQuantity(updatedOrderItem.getQuantity());

        double newTotal = newMenuItem.getPrice() * updatedOrderItem.getQuantity();

        OrderItem savedItem = orderItemRepository.save(oldItem);

        newOrder.setTotalPrice(newOrder.getTotalPrice() + newTotal);
        orderRepository.save(newOrder);

        return savedItem;
    }

    public void deleteOrderItem(Long id) {
        validateId(id);

        OrderItem orderItem = getOrderItemById(id);

        Order order = orderItem.getOrder();
        double itemTotal = orderItem.getMenuItem().getPrice() * orderItem.getQuantity();

        order.setTotalPrice(Math.max(0, order.getTotalPrice() - itemTotal));
        orderRepository.save(order);

        orderItemRepository.deleteById(id);
    }

    private void validateOrderItem(OrderItem orderItem) {
        if (orderItem.getOrder() == null || orderItem.getOrder().getId() == null) {
            throw new BadRequestException("Order is required");
        }

        if (orderItem.getMenuItem() == null || orderItem.getMenuItem().getId() == null) {
            throw new BadRequestException("Menu item is required");
        }

        if (orderItem.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
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