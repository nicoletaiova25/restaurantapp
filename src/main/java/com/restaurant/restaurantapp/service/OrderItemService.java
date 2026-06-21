package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import com.restaurant.restaurantapp.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public OrderItemService(OrderItemRepository orderItemRepository, OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
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

        if (!"OPEN".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("You can add items only to an OPEN order");
        }

        double itemTotal = orderItem.getMenuItem().getPrice() * orderItem.getQuantity();

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

        oldOrder.setTotalPrice(oldOrder.getTotalPrice() - oldTotal);
        orderRepository.save(oldOrder);

        Order newOrder = orderRepository.findById(updatedOrderItem.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", updatedOrderItem.getOrder().getId()));

        if (!"OPEN".equalsIgnoreCase(newOrder.getStatus())) {
            throw new BadRequestException("You can update items only for an OPEN order");
        }

        oldItem.setOrder(updatedOrderItem.getOrder());
        oldItem.setMenuItem(updatedOrderItem.getMenuItem());
        oldItem.setQuantity(updatedOrderItem.getQuantity());

        double newTotal = updatedOrderItem.getMenuItem().getPrice() * updatedOrderItem.getQuantity();

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