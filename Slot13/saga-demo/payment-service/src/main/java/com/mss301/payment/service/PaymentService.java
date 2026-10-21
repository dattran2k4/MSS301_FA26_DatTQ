package com.mss301.payment.service;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.Payment;
import com.mss301.payment.entity.PaymentStatus;
import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment pay(PaymentRequest request) {
        var existing = paymentRepository.findById(request.orderId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = walletRepository.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy ví của " + request.customerId()));
        if (wallet.getBalance() < request.amount()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Số dư không đủ: còn " + wallet.getBalance() + ", cần " + request.amount());
        }

        // JPA dirty checking persists the balance in the same local transaction.
        wallet.setBalance(wallet.getBalance() - request.amount());
        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.PAID);
        payment = paymentRepository.save(payment);
        log.info("PAY order={} customer={} amount={} -> balance={}",
                request.orderId(), request.customerId(), request.amount(), wallet.getBalance());
        return payment;
    }

    @Transactional
    public void refund(Long orderId) {
        var existing = paymentRepository.findById(orderId);
        if (existing.isEmpty() || existing.get().getStatus() == PaymentStatus.REFUNDED) {
            log.info("REFUND order={} -> không có gì để hoàn tác", orderId);
            return;
        }

        Payment payment = existing.get();
        Wallet wallet = walletRepository.findById(payment.getCustomerId()).orElseThrow();
        wallet.setBalance(wallet.getBalance() + payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("REFUND order={} -> hoàn {} cho {}, balance={}",
                orderId, payment.getAmount(), payment.getCustomerId(), wallet.getBalance());
    }
}
