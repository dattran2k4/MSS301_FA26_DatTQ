package com.mss301.payment.service;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.Payment;
import com.mss301.payment.entity.PaymentStatus;
import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private WalletRepository wallets;
    @Mock
    private PaymentRepository payments;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(wallets, payments);
    }

    @Test
    void payDebitsOnceAndReturnsExistingPaymentOnRetry() {
        Wallet wallet = new Wallet("C1", 5000);
        when(wallets.findById("C1")).thenReturn(Optional.of(wallet));
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        PaymentRequest request = new PaymentRequest(1L, "C1", 2000);

        Payment result = service.pay(request);
        assertEquals(3000, wallet.getBalance());
        assertEquals(1L, result.getOrderId());
        assertEquals("C1", result.getCustomerId());
        assertEquals(2000, result.getAmount());
        assertEquals(PaymentStatus.PAID, result.getStatus());

        when(payments.findById(1L)).thenReturn(Optional.of(result));
        assertSame(result, service.pay(request));
        assertEquals(3000, wallet.getBalance());
        verify(payments, times(1)).save(any(Payment.class));
        verify(wallets, times(1)).findById("C1");
    }

    @Test
    void insufficientBalanceReturns402WithoutDebitingOrSaving() {
        Wallet wallet = new Wallet("C2", 100);
        when(wallets.findById("C2")).thenReturn(Optional.of(wallet));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.pay(new PaymentRequest(2L, "C2", 1000)));
        assertEquals(402, error.getStatusCode().value());
        assertTrue(error.getReason().startsWith("Số dư không đủ"));
        assertEquals(100, wallet.getBalance());
        verify(payments, never()).save(any());
    }

    @Test
    void missingWalletReturns404WithoutSaving() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.pay(new PaymentRequest(3L, "missing", 20)));
        assertEquals(404, error.getStatusCode().value());
        verify(payments, never()).save(any());
    }

    @Test
    void refundTwiceCreditsOnlyOnce() {
        Wallet wallet = new Wallet("C1", 3000);
        Payment payment = paidPayment();
        when(payments.findById(1L)).thenReturn(Optional.of(payment));
        when(wallets.findById("C1")).thenReturn(Optional.of(wallet));

        service.refund(1L);
        assertEquals(5000, wallet.getBalance());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        service.refund(1L);
        assertEquals(5000, wallet.getBalance());
        verify(wallets, times(1)).findById("C1");
    }

    @Test
    void refundUnknownOrderDoesNothing() {
        assertDoesNotThrow(() -> service.refund(999L));
        verifyNoInteractions(wallets);
    }

    @Test
    void payAfterRefundDoesNotChargeAgain() {
        Payment payment = paidPayment();
        payment.setStatus(PaymentStatus.REFUNDED);
        when(payments.findById(1L)).thenReturn(Optional.of(payment));

        assertSame(payment, service.pay(new PaymentRequest(1L, "C1", 2000)));
        verifyNoInteractions(wallets);
        verify(payments, never()).save(any());
    }

    private Payment paidPayment() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setCustomerId("C1");
        payment.setAmount(2000);
        payment.setStatus(PaymentStatus.PAID);
        return payment;
    }
}
