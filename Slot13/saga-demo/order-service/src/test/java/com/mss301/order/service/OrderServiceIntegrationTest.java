package com.mss301.order.service;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.SagaLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Use an explicitly supplied disposable MySQL database, never the normal orderdb.
// No test-level transaction: each service call must commit independently.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "SAGA_TEST_DB_URL", matches = ".+")
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService service;
    @Autowired
    private SagaLogRepository logs;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("SAGA_TEST_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("SAGA_TEST_DB_USER", "root"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("SAGA_TEST_DB_PASSWORD", "root"));
    }

    @Test
    void commitsPendingExecutingAndConfirmedStates() {
        Order order = service.createPendingOrder(new CreateOrderRequest("C1", "P1", 2, 2000));
        Long id = order.getId();
        Order persisted = service.findById(id);
        assertEquals("C1", persisted.getCustomerId());
        assertEquals("P1", persisted.getProductId());
        assertEquals(2, persisted.getQuantity());
        assertEquals(2000, persisted.getAmount());
        assertState(id, OrderStatus.PENDING, SagaStatus.PENDING);
        service.updateSagaStatus(id, SagaStatus.EXECUTING);
        assertState(id, OrderStatus.PENDING, SagaStatus.EXECUTING);
        service.confirmOrder(id);
        assertState(id, OrderStatus.CONFIRMED, SagaStatus.COMPLETED);
        assertNull(service.findById(id).getFailureReason());
    }

    @Test
    void commitsFailureCompensationAndOrderedLogs() {
        Long id = service.createPendingOrder(new CreateOrderRequest("C2", "P1", 1, 1000)).getId();
        service.log(id, "CREATE_ORDER", "SUCCESS", "Order PENDING");
        service.markFailed(id, "402 - Số dư không đủ");
        assertState(id, OrderStatus.PENDING, SagaStatus.FAILED);
        service.log(id, "SAGA", "FAILED", "402 - Số dư không đủ");
        service.updateSagaStatus(id, SagaStatus.COMPENSATING);
        assertState(id, OrderStatus.PENDING, SagaStatus.COMPENSATING);
        service.cancelOrder(id);
        service.cancelOrder(id);
        assertState(id, OrderStatus.CANCELLED, SagaStatus.COMPENSATED);
        assertEquals("402 - Số dư không đủ", service.findById(id).getFailureReason());
        var savedLogs = logs.findByOrderIdOrderByIdAsc(id);
        assertEquals(List.of("CREATE_ORDER", "SAGA"), savedLogs.stream().map(l -> l.getStep()).toList());
        assertEquals("FAILED", savedLogs.get(1).getResult());
        assertEquals("402 - Số dư không đủ", savedLogs.get(1).getMessage());
        assertNotNull(savedLogs.get(0).getTime());
    }

    @Test
    void failingLocalTransactionDoesNotUndoEarlierCommits() {
        Long id = service.createPendingOrder(new CreateOrderRequest("C1", "P2", 1, 20)).getId();
        service.updateSagaStatus(id, SagaStatus.EXECUTING);
        service.log(id, "CREATE_ORDER", "SUCCESS", "Order persisted");
        // Exceed the real MySQL column size to force a commit failure.
        assertThrows(RuntimeException.class, () -> service.markFailed(id, "x".repeat(1001)));
        assertState(id, OrderStatus.PENDING, SagaStatus.EXECUTING);
        assertNull(service.findById(id).getFailureReason());
        assertEquals(1, logs.findByOrderIdOrderByIdAsc(id).size());
    }

    private void assertState(Long id, OrderStatus status, SagaStatus sagaStatus) {
        Order persisted = service.findById(id);
        assertEquals(status, persisted.getStatus());
        assertEquals(sagaStatus, persisted.getSagaStatus());
    }
}
