package com.mss301.inventory.service;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.entity.Product;
import com.mss301.inventory.entity.Reservation;
import com.mss301.inventory.entity.ReservationStatus;
import com.mss301.inventory.repository.ProductRepository;
import com.mss301.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation reserve(ReserveRequest request) {
        var existing = reservationRepository.findById(request.orderId());
        if (existing.isPresent()) {
            log.info("RESERVE order={} đã xử lý -> bỏ qua", request.orderId());
            return existing.get();
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm " + request.productId()));
        if (product.getStock() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không đủ hàng: còn " + product.getStock() + ", cần " + request.quantity());
        }

        product.setStock(product.getStock() - request.quantity());
        productRepository.save(product);

        Reservation reservation = new Reservation();
        reservation.setOrderId(request.orderId());
        reservation.setProductId(request.productId());
        reservation.setQuantity(request.quantity());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation = reservationRepository.save(reservation);
        log.info("RESERVE order={} product={} qty={} -> stock={}",
                request.orderId(), product.getId(), request.quantity(), product.getStock());
        return reservation;
    }

    @Transactional
    public void release(Long orderId) {
        var existing = reservationRepository.findById(orderId);
        if (existing.isEmpty() || existing.get().getStatus() == ReservationStatus.RELEASED) {
            log.info("RELEASE order={} -> không có gì để hoàn tác", orderId);
            return;
        }

        Reservation reservation = existing.get();
        Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
        product.setStock(product.getStock() + reservation.getQuantity());
        reservation.setStatus(ReservationStatus.RELEASED);
        productRepository.save(product);
        reservationRepository.save(reservation);
        log.info("RELEASE order={} -> trả lại {} {}, stock={}",
                orderId, reservation.getQuantity(), product.getId(), product.getStock());
    }
}
