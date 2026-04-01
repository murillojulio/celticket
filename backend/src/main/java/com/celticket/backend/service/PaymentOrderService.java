package com.celticket.backend.service;

import com.celticket.backend.dto.CheckoutRequest;
import com.celticket.backend.dto.CheckoutQuoteResponse;
import com.celticket.backend.model.PaymentOrder;
import com.celticket.backend.repository.PaymentOrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper;

    public PaymentOrderService(PaymentOrderRepository paymentOrderRepository, ObjectMapper objectMapper) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.objectMapper = objectMapper;
    }

    public PaymentOrder createPendingWompiOrder(CheckoutRequest request, CheckoutQuoteResponse quote) {
        PaymentOrder order = new PaymentOrder();
        order.setProvider("WOMPI");
        order.setReference(generateReference(request.getEventId()));
        order.setEventId(request.getEventId());
        order.setSessionId(request.getSessionId());
        order.setBuyerEmail(request.getBuyerEmail());
        order.setAmount(quote.getTotal());
        order.setSubtotal(quote.getSubtotal());
        order.setServiceFee(quote.getServiceFee());
        order.setCurrency(quote.getCurrency());
        order.setStatus("PENDING");
        order.setSeatIdsJson(writeSeatIds(request.getSeatIds()));
        order.setItemsJson(writeItems(quote.getItems()));
        return paymentOrderRepository.save(order);
    }

    public Optional<PaymentOrder> findByReference(String reference) {
        return paymentOrderRepository.findByReference(reference);
    }

    public List<String> readSeatIds(PaymentOrder order) {
        if (order.getSeatIdsJson() == null || order.getSeatIdsJson().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(order.getSeatIdsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron leer los asientos de la orden " + order.getReference(), e);
        }
    }

    public PaymentOrder markApproved(PaymentOrder order, String providerTransactionId) {
        order.setStatus("APPROVED");
        order.setProviderTransactionId(providerTransactionId);
        order.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(order);
    }

    public PaymentOrder markFailed(PaymentOrder order, String status, String providerTransactionId) {
        order.setStatus(status);
        order.setProviderTransactionId(providerTransactionId);
        order.setUpdatedAt(LocalDateTime.now());
        return paymentOrderRepository.save(order);
    }

    private String writeSeatIds(List<String> seatIds) {
        try {
            return objectMapper.writeValueAsString(seatIds);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron serializar los asientos para la orden de pago.", e);
        }
    }

    private String writeItems(List<CheckoutQuoteResponse.SeatPriceItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el detalle de precios.", e);
        }
    }

    private String generateReference(Long eventId) {
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "CELT-" + eventId + "-" + rnd;
    }
}
