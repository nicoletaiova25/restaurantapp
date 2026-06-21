package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.Payment;
import com.restaurant.restaurantapp.repository.OrderRepository;
import com.restaurant.restaurantapp.repository.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        validateId(id);
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }

    public Payment createPayment(Payment payment) {
        validatePayload(payment, "Payment payload is required");
        validatePayment(payment);

        Long orderId = payment.getOrder().getId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        payment.setOrder(order);

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.isPaid()) {
            order.setStatus("PAID");
            orderRepository.save(order);
        }

        return savedPayment;
    }

    public Payment updatePayment(Long id, Payment updatedPayment) {
        validatePayload(updatedPayment, "Updated payment payload is required");
        validatePayment(updatedPayment);

        Payment payment = getPaymentById(id);

        Long orderId = updatedPayment.getOrder().getId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        payment.setOrder(order);
        payment.setMethod(updatedPayment.getMethod());
        payment.setPaid(updatedPayment.isPaid());

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.isPaid()) {
            order.setStatus("PAID");
        } else {
            order.setStatus("OPEN");
        }

        orderRepository.save(order);

        return savedPayment;
    }

    public void deletePayment(Long id) {
        validateId(id);

        Payment payment = getPaymentById(id);

        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus("OPEN");
            orderRepository.save(order);
        }

        paymentRepository.deleteById(id);
    }

    private void validatePayment(Payment payment) {
        if (payment.getOrder() == null || payment.getOrder().getId() == null) {
            throw new BadRequestException("Order is required");
        }

        if (payment.getMethod() == null || payment.getMethod().isBlank()) {
            throw new BadRequestException("Payment method is required");
        }

        String method = payment.getMethod().trim().toUpperCase();

        if (!method.equals("CASH") && !method.equals("CARD")) {
            throw new BadRequestException("Payment method must be CASH or CARD");
        }

        payment.setMethod(method);
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