package com.celticket.backend.service;

import com.celticket.backend.dto.CheckoutQuoteResponse;
import com.celticket.backend.model.Event;
import com.celticket.backend.repository.EventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SeatPricingService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${payments.default-seat-price-cents:20000}")
    private Integer defaultSeatPriceCents;

    @Value("${payments.service-fee-cents:0}")
    private Integer serviceFeeCents;

    @Value("${wompi.currency:COP}")
    private String currency;

    public SeatPricingService(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public CheckoutQuoteResponse quote(Long eventId, List<String> seatIds) {
        if (eventId == null) {
            throw new IllegalStateException("eventId es obligatorio.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Evento no encontrado: " + eventId));

        Map<String, Integer> sectionPrices = extractSectionPrices(event.getLayoutConfig());
        int defaultPrice = safeDefaultSeatPrice();
        int fee = safeServiceFee();
        List<CheckoutQuoteResponse.SeatPriceItem> items = new ArrayList<>();
        int subtotal = 0;

        for (String seatId : seatIds) {
            String section = extractSectionFromSeatId(seatId);
            int unitPrice = sectionPrices.getOrDefault(section, defaultPrice);
            subtotal += unitPrice;
            items.add(new CheckoutQuoteResponse.SeatPriceItem(seatId, section, unitPrice));
        }

        CheckoutQuoteResponse response = new CheckoutQuoteResponse();
        response.setCurrency(currency);
        response.setSubtotalCents(subtotal);
        response.setServiceFeeCents(fee);
        response.setTotalCents(subtotal + fee);
        response.setItems(items);
        return response;
    }

    private Map<String, Integer> extractSectionPrices(String layoutConfig) {
        Map<String, Integer> sectionPrices = new HashMap<>();
        int defaultPrice = safeDefaultSeatPrice();
        if (layoutConfig == null || layoutConfig.isBlank()) {
            return sectionPrices;
        }
        try {
            JsonNode root = objectMapper.readTree(layoutConfig);
            JsonNode sections = root.path("sections");
            if (!sections.isArray()) {
                return sectionPrices;
            }
            for (JsonNode section : sections) {
                String name = section.path("name").asText(null);
                Integer price = section.has("basePriceCents") ? section.path("basePriceCents").asInt(defaultPrice) : defaultPrice;
                if (name != null && !name.isBlank()) {
                    sectionPrices.put(name, price);
                }
            }
        } catch (Exception ignored) {
            // Si no se puede parsear, se usa precio por defecto.
        }
        return sectionPrices;
    }

    private String extractSectionFromSeatId(String seatId) {
        if (seatId == null || seatId.isBlank()) {
            return "";
        }
        int lastDash = seatId.lastIndexOf('-');
        if (lastDash <= 0) {
            return "";
        }
        return seatId.substring(0, lastDash);
    }

    private int safeDefaultSeatPrice() {
        if (defaultSeatPriceCents == null) return 20000;
        return defaultSeatPriceCents > 0 ? defaultSeatPriceCents : 20000;
    }

    private int safeServiceFee() {
        if (serviceFeeCents == null) return 0;
        int fee = serviceFeeCents;
        return Math.max(fee, 0);
    }
}
