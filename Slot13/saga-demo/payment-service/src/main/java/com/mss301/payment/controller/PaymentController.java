package com.mss301.payment.controller;

import com.mss301.payment.entity.Wallet;
import com.mss301.payment.entity.Payment;
import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import com.mss301.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PostMapping("/pay")
    public Payment pay(@RequestBody PaymentRequest request) {
        return paymentService.pay(request);
    }

    @PostMapping("/refund/{orderId}")
    public String refund(@PathVariable Long orderId) {
        paymentService.refund(orderId);
        return "REFUNDED";
    }

    @GetMapping
    public List<Payment> payments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/wallets")
    public List<Wallet> wallets() {
        return walletRepository.findAll();
    }
}
