package com.celticket.backend.controller;

import com.celticket.backend.dto.AdminOrderDto;
import com.celticket.backend.dto.PagedResponse;
import com.celticket.backend.model.PaymentOrder;
import com.celticket.backend.repository.PaymentOrderRepository;
import com.celticket.backend.service.PaymentOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*")
public class AdminOrderController {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOrderService paymentOrderService;

    public AdminOrderController(PaymentOrderRepository paymentOrderRepository,
                                PaymentOrderService paymentOrderService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentOrderService = paymentOrderService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AdminOrderDto>> list(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long eventFilter = eventId != null ? eventId : null;
        String statusFilter = StringUtils.hasText(status) ? status.trim() : null;
        Page<PaymentOrder> result = paymentOrderRepository.search(
                eventFilter,
                statusFilter,
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        PagedResponse<AdminOrderDto> body = new PagedResponse<>(
                result.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<?> getByReference(@PathVariable String reference) {
        return paymentOrderRepository.findByReference(reference)
                .map(o -> ResponseEntity.ok(toDto(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AdminOrderDto toDto(PaymentOrder order) {
        AdminOrderDto dto = new AdminOrderDto();
        dto.setReference(order.getReference());
        dto.setStatus(order.getStatus());
        dto.setEventId(order.getEventId());
        dto.setAmount(order.getAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setServiceFee(order.getServiceFee());
        dto.setCurrency(order.getCurrency());
        dto.setBuyerEmail(order.getBuyerEmail());
        dto.setSessionId(order.getSessionId());
        dto.setSeatIds(paymentOrderService.readSeatIds(order));
        dto.setProviderTransactionId(order.getProviderTransactionId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}
