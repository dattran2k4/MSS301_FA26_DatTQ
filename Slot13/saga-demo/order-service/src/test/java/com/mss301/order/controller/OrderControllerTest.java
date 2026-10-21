package com.mss301.order.controller;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import com.mss301.order.saga.OrderSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {
    private OrderSagaOrchestrator saga;
    private OrderRepository orders;
    private SagaLogRepository logs;
    private MockMvc mvc;
    private final CreateOrderRequest request = new CreateOrderRequest("C1", "P1", 2, 2000);
    private static final String BODY = "{\"customerId\":\"C1\",\"productId\":\"P1\",\"quantity\":2,\"amount\":2000}";

    @BeforeEach
    void setUp() {
        saga = mock(OrderSagaOrchestrator.class);
        orders = mock(OrderRepository.class);
        logs = mock(SagaLogRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(saga, orders, logs)).build();
    }

    @Test
    void postDeserializesRequestAndReturnsCompletedOrder() throws Exception {
        when(saga.placeOrder(request)).thenReturn(order(OrderStatus.CONFIRMED, SagaStatus.COMPLETED));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.customerId").value("C1"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.amount").value(2000))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.sagaStatus").value("COMPLETED"));
        verify(saga).placeOrder(request);
    }

    @Test
    void compensatedOrderReturns200WithFailureReason() throws Exception {
        Order order = order(OrderStatus.CANCELLED, SagaStatus.COMPENSATED);
        order.setFailureReason("402 - Số dư không đủ");
        when(saga.placeOrder(request)).thenReturn(order);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.sagaStatus").value("COMPENSATED"))
                .andExpect(jsonPath("$.failureReason").value("402 - Số dư không đủ"));
    }

    @Test
    void listsOrdersWithoutRunningSaga() throws Exception {
        when(orders.findAll()).thenReturn(List.of(order(OrderStatus.PENDING, SagaStatus.COMPENSATING)));
        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].sagaStatus").value("COMPENSATING"));
        verifyNoInteractions(saga);
    }

    @Test
    void returnsLogsForRequestedOrderInRepositoryOrder() throws Exception {
        SagaLog first = new SagaLog(7L, "CREATE_ORDER", "SUCCESS", "Order PENDING");
        first.setTime(LocalDateTime.of(2026, 10, 22, 7, 0));
        SagaLog second = new SagaLog(7L, "CONFIRM_ORDER", "SUCCESS", "Saga COMPLETED");
        when(logs.findByOrderIdOrderByIdAsc(7L)).thenReturn(List.of(first, second));
        mvc.perform(get("/api/orders/7/saga-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value(7))
                .andExpect(jsonPath("$[0].step").value("CREATE_ORDER"))
                .andExpect(jsonPath("$[0].time").value("2026-10-22T07:00:00"))
                .andExpect(jsonPath("$[1].step").value("CONFIRM_ORDER"));
        verify(logs).findByOrderIdOrderByIdAsc(7L);
        verifyNoInteractions(saga);
    }

    private Order order(OrderStatus status, SagaStatus sagaStatus) {
        Order order = new Order();
        order.setId(7L);
        order.setCustomerId("C1");
        order.setProductId("P1");
        order.setQuantity(2);
        order.setAmount(2000);
        order.setStatus(status);
        order.setSagaStatus(sagaStatus);
        return order;
    }
}
