package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.Payment;
import com.restaurant.restaurantapp.repository.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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
        return paymentRepository.save(payment);
    }

    public Payment updatePayment(Long id, Payment updatedPayment) {
        validatePayload(updatedPayment, "Updated payment payload is required");
        Payment payment = getPaymentById(id);
        payment.setOrder(updatedPayment.getOrder());
        payment.setMethod(updatedPayment.getMethod());
        payment.setPaid(updatedPayment.isPaid());
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        validateId(id);
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment", "id", id);
        }
        paymentRepository.deleteById(id);
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
