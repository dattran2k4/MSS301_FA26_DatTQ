package com.mss301.order.saga;

import com.mss301.order.client.InventoryClient;
import com.mss301.order.client.PaymentClient;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderSagaOrchestratorTest {
    private OrderService orders;
    private InventoryClient inventory;
    private PaymentClient payment;
    private OrderSagaOrchestrator saga;
    private final CreateOrderRequest request = new CreateOrderRequest("C1", "P1", 2, 2000);
    private Order order;

    @BeforeEach
    void setUp() {
        orders = mock(OrderService.class);
        inventory = mock(InventoryClient.class);
        payment = mock(PaymentClient.class);
        saga = new OrderSagaOrchestrator(orders, inventory, payment);
        order = new Order();
        order.setId(1L);
        when(orders.createPendingOrder(request)).thenReturn(order);
        when(orders.findById(1L)).thenReturn(order);
    }

    @Test
    void happyPathRunsInOrderWithoutCompensation() {
        assertSame(order, saga.placeOrder(request));
        var sequence = inOrder(orders, inventory, payment);
        sequence.verify(orders).createPendingOrder(request);
        sequence.verify(orders).log(1L, "CREATE_ORDER", "SUCCESS", "Order PENDING");
        sequence.verify(orders).updateSagaStatus(1L, SagaStatus.EXECUTING);
        sequence.verify(inventory).reserve(1L, "P1", 2);
        sequence.verify(orders).log(eq(1L), eq("RESERVE_INVENTORY"), eq("SUCCESS"), anyString());
        sequence.verify(payment).pay(1L, "C1", 2000);
        sequence.verify(orders).log(eq(1L), eq("PROCESS_PAYMENT"), eq("SUCCESS"), anyString());
        sequence.verify(orders).confirmOrder(1L);
        sequence.verify(orders).log(1L, "CONFIRM_ORDER", "SUCCESS", "Saga COMPLETED");
        verify(inventory, never()).release(anyLong());
        verify(payment, never()).refund(anyLong());
        verify(orders, never()).cancelOrder(anyLong());
    }

    @Test
    void inventoryFailureCancelsWithoutReleaseOrPayment() {
        doThrow(new HttpClientErrorException(HttpStatus.CONFLICT))
                .when(inventory).reserve(1L, "P1", 2);
        saga.placeOrder(request);
        verify(orders).markFailed(eq(1L), startsWith("409"));
        verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        verify(orders).cancelOrder(1L);
        verify(inventory, never()).release(anyLong());
        verifyNoInteractions(payment);
    }

    @Test
    void paymentFailureReleasesBeforeCancellingAndPreservesErrorMessage() {
        var error = HttpClientErrorException.create(HttpStatus.PAYMENT_REQUIRED, "Payment Required",
                HttpHeaders.EMPTY, "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        error.setBodyConvertFunction(type -> Map.of("message", "Số dư không đủ"));
        doThrow(error).when(payment).pay(1L, "C1", 2000);
        saga.placeOrder(request);
        var sequence = inOrder(orders, inventory);
        sequence.verify(orders).markFailed(1L, "402 - Số dư không đủ");
        sequence.verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).log(1L, "COMPENSATION", "SUCCESS", "Lần thử 1");
        sequence.verify(orders).cancelOrder(1L);
        sequence.verify(orders).log(1L, "CANCEL_ORDER", "SUCCESS", "Saga COMPENSATED");
        verify(payment, never()).refund(anyLong());
    }

    @Test
    void networkFailureIncludesReadableReasonAndCompensates() {
        doThrow(new ResourceAccessException("Connection refused")).when(payment).pay(1L, "C1", 2000);
        saga.placeOrder(request);
        verify(orders).markFailed(1L, "Service không phản hồi: Connection refused");
        verify(inventory).release(1L);
        verify(orders).cancelOrder(1L);
    }

    @Test
    void compensationSucceedsOnThirdAttemptThenCancels() {
        doThrow(new ResourceAccessException("offline")).when(payment).pay(1L, "C1", 2000);
        doThrow(new ResourceAccessException("offline"))
                .doThrow(new ResourceAccessException("offline")).doNothing().when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(3)).release(1L);
        verify(orders, times(2)).log(eq(1L), eq("COMPENSATION"), eq("FAILED"), anyString());
        verify(orders).log(1L, "COMPENSATION", "SUCCESS", "Lần thử 3");
        verify(orders).cancelOrder(1L);
    }

    @Test
    void exhaustedCompensationStaysCompensatingWithoutCancelling() {
        doThrow(new ResourceAccessException("offline")).when(payment).pay(1L, "C1", 2000);
        doThrow(new ResourceAccessException("offline")).when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(3)).release(1L);
        verify(orders, times(3)).log(eq(1L), eq("COMPENSATION"), eq("FAILED"), anyString());
        verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        verify(orders, never()).cancelOrder(anyLong());
        verify(orders, never()).log(eq(1L), eq("CANCEL_ORDER"), anyString(), anyString());
    }

    @Test
    void compensationStackRunsRefundThenReleaseThenCancel() {
        // Exercise a stack with both completed remote steps, as needed by EXT-1.
        Deque<Runnable> actions = new ArrayDeque<>();
        actions.push(() -> inventory.release(1L));
        actions.push(() -> payment.refund(1L));
        ReflectionTestUtils.invokeMethod(saga, "compensate", 1L, actions);
        var sequence = inOrder(payment, inventory, orders);
        sequence.verify(payment).refund(1L);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).cancelOrder(1L);
        assertTrue(actions.isEmpty());
    }
}
