package com.restaurant.restaurantapp.service;

import static com.restaurant.restaurantapp.TestFixtures.order;
import static com.restaurant.restaurantapp.TestFixtures.payment;
import static com.restaurant.restaurantapp.TestFixtures.restaurantTable;
import static com.restaurant.restaurantapp.TestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.restaurant.restaurantapp.model.Payment;
import com.restaurant.restaurantapp.repository.OrderRepository;
import com.restaurant.restaurantapp.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = order(
                restaurantTable(8, 4, user("waiter", "pw", "WAITER")),
                user("waiter", "pw", "WAITER"),
                "OPEN",
                25.0
        );
        order.setId(1L);

        payment = payment(order, "CARD", true);
        payment.setId(1L);
    }

    @Test
    void getAllPaymentsReturnsRepositoryResults() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        List<Payment> payments = paymentService.getAllPayments();

        assertEquals(1, payments.size());
        verify(paymentRepository).findAll();
    }

    @Test
    void getPaymentByIdReturnsPaymentWhenFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(1L);

        assertEquals(payment, result);
        verify(paymentRepository).findById(1L);
    }

    @Test
    void getPaymentByIdRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> paymentService.getPaymentById(-1L));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPaymentByIdThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(99L));
        verify(paymentRepository).findById(99L);
    }

    @Test
    void createPaymentSavesPayload() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment created = paymentService.createPayment(payment);

        assertEquals("CARD", created.getMethod());
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPaymentRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> paymentService.createPayment(null));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updatePaymentAppliesChanges() {
        Order newOrder = order(
                restaurantTable(10, 6, user("new", "pw", "WAITER")),
                user("new", "pw", "WAITER"),
                "PAID",
                55.0
        );
        newOrder.setId(2L);

        Payment updatedPayment = payment(newOrder, "CASH", false);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(newOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.updatePayment(1L, updatedPayment);

        assertEquals("CASH", result.getMethod());
        assertFalse(result.isPaid());
        verify(paymentRepository).save(payment);
    }

    @Test
    void updatePaymentRejectsNullPayload() {
        assertThrows(BadRequestException.class, () -> paymentService.updatePayment(1L, null));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void deletePaymentDeletesWhenPresent() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(1L);

        verify(paymentRepository).deleteById(1L);
    }

    @Test
    void deletePaymentThrowsWhenMissing() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.deletePayment(1L));
        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).deleteById(anyLong());
    }

    @Test
    void createPaymentValidatesMethod() {
        Payment invalidPayment = payment(order, "BITCOIN", false);

        assertThrows(BadRequestException.class, () -> paymentService.createPayment(invalidPayment));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentRejectsBlankMethod() {
        Payment invalidPayment = payment(order, "  ", false);

        assertThrows(BadRequestException.class, () -> paymentService.createPayment(invalidPayment));
    }

    @Test
    void createPaymentRejectsNullMethod() {
        Payment invalidPayment = payment(order, null, false);

        assertThrows(BadRequestException.class, () -> paymentService.createPayment(invalidPayment));
    }

    @Test
    void createPaymentRejectsNullOrder() {
        Payment invalidPayment = new Payment();
        invalidPayment.setMethod("CARD");
        invalidPayment.setOrder(null);

        assertThrows(BadRequestException.class, () -> paymentService.createPayment(invalidPayment));
    }

    @Test
    void createPaymentSetsOrderStatusToPaidWhenPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(payment);

        verify(orderRepository).save(order);
    }

    @Test
    void updatePaymentNormalizesMethod() {
        Payment updated = payment(order, "cash", false);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.updatePayment(1L, updated);

        assertEquals("CASH", result.getMethod());
    }

    @Test
    void updatePaymentSetsStatusOpenWhenNotPaid() {
        order.setStatus("PAID");

        Payment updated = payment(order, "CARD", false);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.updatePayment(1L, updated);

        verify(orderRepository).save(order);
    }

    @Test
    void deletePaymentResetsOrderStatusToOpen() {
        order.setStatus("PAID");
        payment.setOrder(order);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(1L);

        verify(orderRepository).save(order);
        verify(paymentRepository).deleteById(1L);
    }

    @Test
    void getPaymentByIdRejectsNullId() {
        assertThrows(BadRequestException.class, () -> paymentService.getPaymentById(null));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void deletePaymentRejectsInvalidId() {
        assertThrows(BadRequestException.class, () -> paymentService.deletePayment(0L));
        verifyNoInteractions(paymentRepository);
    }
}