package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        validateId(id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    public Order createOrder(Order order) {
        validatePayload(order, "Order payload is required");

        if (order.getTable() == null || order.getTable().getId() == null) {
            throw new BadRequestException("Table is required");
        }

        if (order.getWaiter() == null || order.getWaiter().getId() == null) {
            throw new BadRequestException("Waiter is required");
        }

        order.setStatus("OPEN");
        order.setTotalPrice(0.0);

        return orderRepository.save(order);
    }

    public Order updateOrder(Long id, Order updatedOrder) {
        validatePayload(updatedOrder, "Updated order payload is required");

        Order order = getOrderById(id);

        order.setTable(updatedOrder.getTable());
        order.setWaiter(updatedOrder.getWaiter());

        if (updatedOrder.getStatus() != null && !updatedOrder.getStatus().isBlank()) {
            order.setStatus(updatedOrder.getStatus());
        }

        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        validateId(id);

        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", "id", id);
        }

        orderRepository.deleteById(id);
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