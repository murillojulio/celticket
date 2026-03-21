package com.celticket.backend.controller;

import com.celticket.backend.dto.CheckoutRequest;
import com.celticket.backend.dto.CheckoutQuoteResponse;
import com.celticket.backend.dto.WompiCheckoutSessionResponse;
import com.celticket.backend.model.PaymentOrder;
import com.celticket.backend.service.PaymentOrderService;
import com.celticket.backend.service.SeatPricingService;
import com.celticket.backend.service.SeatLockService;
import com.celticket.backend.service.WompiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/wompi")
@CrossOrigin(origins = "*")
public class WompiPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(WompiPaymentController.class);

    private final SeatLockService seatLockService;
    private final PaymentOrderService paymentOrderService;
    private final SeatPricingService seatPricingService;
    private final WompiService wompiService;

    public WompiPaymentController(SeatLockService seatLockService,
                                  PaymentOrderService paymentOrderService,
                                  SeatPricingService seatPricingService,
                                  WompiService wompiService) {
        this.seatLockService = seatLockService;
        this.paymentOrderService = paymentOrderService;
        this.seatPricingService = seatPricingService;
        this.wompiService = wompiService;
    }

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody CheckoutRequest request) {
        if (request.getEventId() == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Faltan datos para cotizar."));
        }
        try {
            CheckoutQuoteResponse quote = seatPricingService.quote(request.getEventId(), request.getSeatIds());
            return ResponseEntity.ok(quote);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cotizando asientos", e);
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo calcular la cotización."));
        }
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutRequest request) {
        if (request.getEventId() == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()
                || request.getSessionId() == null || request.getSessionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Faltan datos para iniciar el checkout."));
        }

        try {
            seatLockService.validateSeatOwnership(request.getEventId(), request.getSeatIds(), request.getSessionId());
            CheckoutQuoteResponse quote = seatPricingService.quote(request.getEventId(), request.getSeatIds());
            PaymentOrder order = paymentOrderService.createPendingWompiOrder(request, quote);
            WompiCheckoutSessionResponse session = wompiService.buildCheckoutSession(order);
            session.setSubtotalCents(quote.getSubtotalCents());
            session.setServiceFeeCents(quote.getServiceFeeCents());
            session.setItems(quote.getItems());
            return ResponseEntity.ok(session);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creando checkout-session Wompi", e);
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo crear la sesión de pago."));
        }
    }

    @GetMapping("/orders/{reference}")
    public ResponseEntity<?> getOrder(@PathVariable String reference) {
        Optional<PaymentOrder> maybeOrder = paymentOrderService.findByReference(reference);
        if (maybeOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PaymentOrder order = maybeOrder.get();
        Map<String, Object> response = new HashMap<>();
        response.put("reference", order.getReference());
        response.put("status", order.getStatus());
        response.put("eventId", order.getEventId());
        List<String> seatIds = paymentOrderService.readSeatIds(order);
        response.put("seatIds", seatIds);
        response.put("seatCount", seatIds.size());
        response.put("subtotalCents", order.getSubtotalCents());
        response.put("serviceFeeCents", order.getServiceFeeCents());
        response.put("amountInCents", order.getAmountInCents());
        response.put("currency", order.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> data = asMap(payload.get("data"));
            if (data == null) return ResponseEntity.ok(Map.of("mensaje", "Webhook recibido sin data."));
            Map<String, Object> transaction = asMap(data.get("transaction"));
            if (transaction == null) return ResponseEntity.ok(Map.of("mensaje", "Webhook recibido sin transaction."));

            String transactionId = stringValue(transaction.get("id"));
            String status = stringValue(transaction.get("status"));
            String reference = stringValue(transaction.get("reference"));
            if (transactionId == null || reference == null) {
                return ResponseEntity.ok(Map.of("mensaje", "Webhook ignorado: payload incompleto."));
            }

            // Verificación adicional contra API de Wompi para evitar confiar ciegamente en payload.
            Map<String, Object> wompiData = wompiService.fetchTransactionById(transactionId);
            Map<String, Object> verifiedTx = wompiData == null ? null : asMap(wompiData.get("data"));
            if (verifiedTx != null) {
                status = stringValue(verifiedTx.get("status"));
                reference = stringValue(verifiedTx.get("reference"));
            }

            Optional<PaymentOrder> maybeOrder = paymentOrderService.findByReference(reference);
            if (maybeOrder.isEmpty()) {
                return ResponseEntity.ok(Map.of("mensaje", "Orden no encontrada para referencia " + reference));
            }

            PaymentOrder order = maybeOrder.get();
            if ("APPROVED".equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.ok(Map.of("mensaje", "Orden ya estaba aprobada."));
            }

            if ("APPROVED".equalsIgnoreCase(status)) {
                List<String> seatIds = paymentOrderService.readSeatIds(order);
                seatLockService.confirmPurchaseBatch(order.getEventId(), seatIds, order.getSessionId(), order.getBuyerEmail());
                paymentOrderService.markApproved(order, transactionId);
                return ResponseEntity.ok(Map.of("mensaje", "Pago aprobado y compra confirmada."));
            }

            if (status == null || status.isBlank()) {
                status = "UNKNOWN";
            }
            paymentOrderService.markFailed(order, status.toUpperCase(), transactionId);
            return ResponseEntity.ok(Map.of("mensaje", "Orden actualizada a estado " + status));
        } catch (Exception e) {
            logger.error("Error procesando webhook Wompi", e);
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "Error procesando webhook"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        String str = String.valueOf(value);
        return str.isBlank() ? null : str;
    }
}
