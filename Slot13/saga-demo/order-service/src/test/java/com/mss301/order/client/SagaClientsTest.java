package com.mss301.order.client;

import com.mss301.order.config.RestClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SagaClientsTest {
    private MockRestServiceServer inventoryServer;
    private MockRestServiceServer paymentServer;
    private InventoryClient inventory;
    private PaymentClient payment;

    @BeforeEach
    void setUp() {
        var config = new RestClientConfig();
        var inventoryBuilder = RestClient.builder();
        var paymentBuilder = RestClient.builder();
        inventoryServer = MockRestServiceServer.bindTo(inventoryBuilder).build();
        paymentServer = MockRestServiceServer.bindTo(paymentBuilder).build();
        inventory = new InventoryClient(config.inventoryRestClient(inventoryBuilder, "http://inventory.test"));
        payment = new PaymentClient(config.paymentRestClient(paymentBuilder, "http://payment.test"));
    }

    @Test
    void sendsCommandsToCorrectServiceWithExpectedJson() {
        inventoryServer.expect(requestTo("http://inventory.test/api/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"orderId\":7,\"productId\":\"P1\",\"quantity\":2}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        inventoryServer.expect(requestTo("http://inventory.test/api/inventory/release/7"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("RELEASED", MediaType.TEXT_PLAIN));
        paymentServer.expect(requestTo("http://payment.test/api/payments/pay"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"orderId\":7,\"customerId\":\"C1\",\"amount\":2000}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        paymentServer.expect(requestTo("http://payment.test/api/payments/refund/7"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("REFUNDED", MediaType.TEXT_PLAIN));

        inventory.reserve(7L, "P1", 2);
        inventory.release(7L);
        payment.pay(7L, "C1", 2000);
        payment.refund(7L);
        inventoryServer.verify();
        paymentServer.verify();
    }

    @ParameterizedTest
    @CsvSource({"reserve,409", "reserve,404", "reserve,500", "release,404", "release,503",
            "pay,402", "pay,404", "pay,500", "refund,404", "refund,503"})
    void propagatesHttpErrorsWithStatusAndBody(String action, int status) {
        var server = server(action);
        String body = "{\"message\":\"Service rejected request\"}";
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON).body(body));

        var error = assertThrows(RestClientResponseException.class, () -> invoke(action));
        assertEquals(status, error.getStatusCode().value());
        assertEquals(body, error.getResponseBodyAsString());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"reserve", "release", "pay", "refund"})
    void propagatesTransportFailureAsRestClientException(String action) {
        var server = server(action);
        server.expect(anything()).andRespond(withException(new IOException("Connection refused")));

        assertThrows(ResourceAccessException.class, () -> invoke(action));
        server.verify();
    }

    private MockRestServiceServer server(String action) {
        return action.equals("reserve") || action.equals("release") ? inventoryServer : paymentServer;
    }

    private void invoke(String action) {
        switch (action) {
            case "reserve" -> inventory.reserve(7L, "P1", 2);
            case "release" -> inventory.release(7L);
            case "pay" -> payment.pay(7L, "C1", 2000);
            case "refund" -> payment.refund(7L);
            default -> throw new IllegalArgumentException(action);
        }
    }
}
