package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
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
        return orderItemRepository.save(orderItem);
    }

    public OrderItem updateOrderItem(Long id, OrderItem updatedOrderItem) {
        validatePayload(updatedOrderItem, "Updated order item payload is required");
        OrderItem orderItem = getOrderItemById(id);
        orderItem.setOrder(updatedOrderItem.getOrder());
        orderItem.setMenuItem(updatedOrderItem.getMenuItem());
        orderItem.setQuantity(updatedOrderItem.getQuantity());
        return orderItemRepository.save(orderItem);
    }

    public void deleteOrderItem(Long id) {
        validateId(id);
        if (!orderItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order item", "id", id);
        }
        orderItemRepository.deleteById(id);
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
