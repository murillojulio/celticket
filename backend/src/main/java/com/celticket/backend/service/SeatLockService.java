package com.celticket.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SeatLockService {

    private static final Logger logger = LoggerFactory.getLogger(SeatLockService.class);
    
    // Tiempo límite que una silla permanece reservada sin pagarse (10 minutos)
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.celticket.backend.repository.SeatRepository seatRepository;
    private final com.celticket.backend.repository.TicketRepository ticketRepository;

    @Autowired
    public SeatLockService(StringRedisTemplate redisTemplate, 
                          org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
                          com.celticket.backend.repository.SeatRepository seatRepository,
                          com.celticket.backend.repository.TicketRepository ticketRepository) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Intenta bloquear una silla de forma atómica en Redis.
     */
    public boolean tryLockSeat(Long eventId, String seatId, String userId) {
        // Verificar primero si ya está vendida en la DB relacional
        com.celticket.backend.model.Seat seat = seatRepository.findByEventIdAndSeatCode(eventId, seatId).orElse(null);
        if (seat != null && "SOLD".equalsIgnoreCase(seat.getStatus())) {
            logger.warn("Attempt to lock already SOLD seat {} for event {}", seatId, eventId);
            return false;
        }

        String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
        
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, userId, LOCK_TIMEOUT);
            logger.info("Lock attempt for seat {} by user {} - Success: {}", seatId, userId, success);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            logger.error("Error connecting to Redis for lock: ", e);
            return false;
        }
    }

    /**
     * Libera (desbloquea) una silla voluntariamente.
     */
    public boolean unlockSeat(Long eventId, String seatId, String userId) {
        String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
        
        try {
            // Verificamos quién es el dueño actual del lock en Redis
            String currentOwner = redisTemplate.opsForValue().get(key);
            
            // Solo puede desbloquearla quien la bloqueó (o un administrador)
            if (userId.equals(currentOwner)) {
                redisTemplate.delete(key);
                logger.info("Seat {} unlocked by user {}", seatId, userId);
                return true;
            }
            logger.warn("User {} tried to unlock seat {} owned by {}", userId, seatId, currentOwner);
            return false;
        } catch (Exception e) {
            logger.error("Error connecting to Redis for unlock: ", e);
            return false;
        }
    }

    /**
     * Obtiene todos los asientos bloqueados actualmente para un evento dado.
     * Retorna un mapa de seatId -> userId
     */
    public java.util.Map<String, String> getAllLockedSeats(Long eventId) {
        java.util.Map<String, String> lockedSeats = new java.util.HashMap<>();
        String pattern = String.format("lock:event:%d:seat:*", eventId);
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    // Extraer seatId de la llave (Ej. "lock:event:105:seat:A1" -> "A1")
                    String seatId = key.substring(key.lastIndexOf(":") + 1);
                    String userId = redisTemplate.opsForValue().get(key);
                    if (userId != null) {
                        lockedSeats.put(seatId, userId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving locked seats from Redis: ", e);
        }
        return lockedSeats;
    }

    /**
     * Obtiene todos los asientos que han sido comprados de forma definitiva.
     * Lee desde el repositorio de MySQL.
     */
    public java.util.Set<String> getSoldSeats(Long eventId) {
        try {
            return seatRepository.findByEventIdAndStatus(eventId, "SOLD")
                    .stream()
                    .map(com.celticket.backend.model.Seat::getSeatCode)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            logger.error("Error retrieving sold seats from MySQL: ", e);
            return new java.util.HashSet<>();
        }
    }
    
    /**
     * Valida que las sillas sigan perteneciendo al sessionId y procesa la compra final.
     */
    @org.springframework.transaction.annotation.Transactional
    public void confirmPurchaseBatch(Long eventId, java.util.List<String> seatIds, String sessionId) {
        confirmPurchaseBatch(eventId, seatIds, sessionId, "purchased@celticket.com");
    }

    /**
     * Valida que las sillas pertenezcan al session actual.
     */
    public void validateSeatOwnership(Long eventId, java.util.List<String> seatIds, String sessionId) {
        
        // 1. Fase de Validación: Verificar que el carrito invitado (sessionId) aún sea dueño de TODAS las sillas en REDIS
        for (String seatId : seatIds) {
            String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
            String currentOwner = redisTemplate.opsForValue().get(key);
            
            if (currentOwner == null) {
                logger.warn("El asiento {} expiró en Redis antes de completarse el pago para la sesión {}", seatId, sessionId);
                throw new IllegalStateException("El tiempo de reserva para la silla " + seatId + " ha expirado. Por favor, vuelve a seleccionarla.");
            }
            
            if (!currentOwner.equals(sessionId)) {
                logger.warn("El asiento {} pertenece a {} pero la sesión {} intentó comprarlo", seatId, currentOwner, sessionId);
                throw new IllegalStateException("La silla " + seatId + " ya no está disponible o pertenece a otro usuario.");
            }
        }
    }
    
    /**
     * Compra definitiva usando email de comprador para trazabilidad real.
     */
    @org.springframework.transaction.annotation.Transactional
    public void confirmPurchaseBatch(Long eventId, java.util.List<String> seatIds, String sessionId, String buyerEmail) {
        validateSeatOwnership(eventId, seatIds, sessionId);
        
        // 2. Fase de Consolidación en SQL: Si todas pasaron, guardar en BD y actualizar estado
        logger.info("VALIDACIÓN CORRECTA. Guardando compra final en MySQL para las sillas {} (Sesión: {})", seatIds, sessionId);
        
        for (String seatId : seatIds) {
            com.celticket.backend.model.Seat seat = seatRepository.findByEventIdAndSeatCode(eventId, seatId)
                    .orElseThrow(() -> new IllegalStateException("Asiento " + seatId + " no encontrado en la base de datos."));
            
            seat.setStatus("SOLD");
            seatRepository.save(seat);
            
            com.celticket.backend.model.Ticket ticket = new com.celticket.backend.model.Ticket(
                    seat,
                    (buyerEmail == null || buyerEmail.isBlank()) ? "purchased@celticket.com" : buyerEmail,
                    sessionId,
                    java.time.LocalDateTime.now()
            );
            ticketRepository.save(ticket);

            // 3. Fase de Limpieza y Broadcasting
            String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
            redisTemplate.delete(key);
            
            // Emitimos explícitamente el estado SOLD para que todos los clientes en vivo pinten la silla de gris (Vendido)
            com.celticket.backend.dto.SeatMessage soldMsg = new com.celticket.backend.dto.SeatMessage(eventId, seatId, "SOLD", sessionId);
            this.messagingTemplate.convertAndSend("/topic/event." + eventId, soldMsg);
        }
        
        logger.info("Compra finalizada exitosamente en MySQL para el evento {}. Asientos: {}", eventId, seatIds);
    }
}
