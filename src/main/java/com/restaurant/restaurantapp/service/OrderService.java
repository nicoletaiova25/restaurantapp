package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders() {
        logger.debug("Loading all orders");
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        logger.debug("Loading order with id {}", id);

        validateId(id);

        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Order with id {} not found", id);
                    return new ResourceNotFoundException("Order", "id", id);
                });
    }

    public Order createOrder(Order order) {

        logger.info("Creating new order");

        validatePayload(order, "Order payload is required");

        if (order.getTable() == null || order.getTable().getId() == null) {
            logger.error("Order creation failed: table is missing");
            throw new BadRequestException("Table is required");
        }

        if (order.getWaiter() == null || order.getWaiter().getId() == null) {
            logger.error("Order creation failed: waiter is missing");
            throw new BadRequestException("Waiter is required");
        }

        order.setStatus("OPEN");
        order.setTotalPrice(0.0);

        Order savedOrder = orderRepository.save(order);

        logger.info("Order created successfully with id {}", savedOrder.getId());

        return savedOrder;
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

        logger.info("Deleting order with id {}", id);

        validateId(id);

        if (!orderRepository.existsById(id)) {
            logger.error("Cannot delete order {} because it does not exist", id);
            throw new ResourceNotFoundException("Order", "id", id);
        }

        orderRepository.deleteById(id);

        logger.info("Order {} deleted successfully", id);
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

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

}