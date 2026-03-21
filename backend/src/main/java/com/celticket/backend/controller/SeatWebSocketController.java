package com.celticket.backend.controller;

import com.celticket.backend.dto.SeatMessage;
import com.celticket.backend.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SeatWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(SeatWebSocketController.class);

    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public SeatWebSocketController(SeatLockService seatLockService, SimpMessagingTemplate messagingTemplate) {
        this.seatLockService = seatLockService;
        this.messagingTemplate = messagingTemplate;
    }

    // El cliente envía a: /app/seat.lock.{eventId}
    @MessageMapping("/seat.lock.{eventId}")
    public void handleLockRequest(@DestinationVariable Long eventId, SeatMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        
        // Extraemos userId del Payload temporalmente si Principal es nulo (para pruebas locales sin JWT auth estricto aún)
        String userId = (principal != null) ? principal.getName() : message.getUserId();
        
        if (userId == null || userId.isEmpty()) {
            logger.warn("Lock request denied: No userId found");
            return;
        }

        String seatId = message.getSeatId();
        logger.info("Received lock request for Event {} Seat {} by User {}", eventId, seatId, userId);

        boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);

        if (locked) {
            // Broadcast a la sala: El asiento fue bloqueado
            SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "LOCKED", userId);
            messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
            logger.info("Broadcasted LOCKED state for seat {} to /topic/event.{}", seatId, eventId);
        } else {
            // El asiento ya estaba bloqueado. Para notificar de vuelta al usuario que falló, 
            // usaríamos colas de usuario privadas: messagingTemplate.convertAndSendToUser(...)
            logger.info("Seat {} lock failed for user {} (already taken)", seatId, userId);
        }
    }

    // El cliente envía a: /app/seat.unlock.{eventId}
    @MessageMapping("/seat.unlock.{eventId}")
    public void handleUnlockRequest(@DestinationVariable Long eventId, SeatMessage message, Principal principal) {
        String userId = (principal != null) ? principal.getName() : message.getUserId();
        String seatId = message.getSeatId();

        logger.info("Received unlock request for Event {} Seat {} by User {}", eventId, seatId, userId);

        boolean unlocked = seatLockService.unlockSeat(eventId, seatId, userId);

        if (unlocked) {
            // Broadcast a la sala: El asiento volvió a estar disponible
            SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "AVAILABLE", null);
            messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
            logger.info("Broadcasted AVAILABLE state for seat {} to /topic/event.{}", seatId, eventId);
        }
    }

    // El cliente envía a: /app/seat.initialState.{eventId}
    @MessageMapping("/seat.initialState.{eventId}")
    public void handleInitialStateRequest(@DestinationVariable Long eventId, SeatMessage message, Principal principal) {
        String userId = (principal != null) ? principal.getName() : message.getUserId();
        
        logger.info("Received initial state request for Event {} by User {}", eventId, userId);
        
        // 1. Enviar Sillas Bloqueadas Temporalmente (LOCKED)
        java.util.Map<String, String> lockedSeats = seatLockService.getAllLockedSeats(eventId);
        for (java.util.Map.Entry<String, String> entry : lockedSeats.entrySet()) {
            SeatMessage initialMsg = new SeatMessage(eventId, entry.getKey(), "LOCKED", entry.getValue());
            initialMsg.setAction("INITIAL_STATE");
            messagingTemplate.convertAndSend("/topic/event." + eventId, initialMsg);
        }
        
        // 2. Enviar Sillas Vendidas Permanentemente (SOLD)
        java.util.Set<String> soldSeats = seatLockService.getSoldSeats(eventId);
        for (String seatId : soldSeats) {
            SeatMessage initialMsg = new SeatMessage(eventId, seatId, "SOLD", null);
            initialMsg.setAction("INITIAL_STATE");
            messagingTemplate.convertAndSend("/topic/event." + eventId, initialMsg);
        }
        
        logger.info("Broadcasted {} locked and {} sold seats for initial state of Event {}", lockedSeats.size(), soldSeats.size(), eventId);
    }
}
