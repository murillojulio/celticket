package com.celticket.backend.controller;

import com.celticket.backend.dto.CheckoutRequest;
import com.celticket.backend.service.SeatLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*") // Para permitir peticiones desde el archivo HTML local
public class CheckoutController {

    private final SeatLockService seatLockService;

    @Autowired
    public CheckoutController(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }

    @PostMapping
    public ResponseEntity<?> processCheckout(@RequestBody CheckoutRequest request) {
        
        try {
            // Pasamos la lista de asientos al servicio para que verifique si el SessionID aún tiene el lock
            // y realice la compra de todos atómicamente o rechace.
            seatLockService.confirmPurchaseBatch(request.getEventId(), request.getSeatIds(), request.getSessionId());
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Sillas compradas exitosamente.");
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            // Excepción lanzada si el tiempo expiró o alguien más tiene la silla
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
