package com.celticket.backend.config;

import com.celticket.backend.dto.SeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisExpirationListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisExpirationListener.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        // Esperamos que la llave tenga este formato: lock:event:{eventId}:seat:{seatId}
        if (expiredKey.startsWith("lock:event:")) {
            logger.info("Keyspace Notification: Key expired: {}", expiredKey);
            
            try {
                // Parseo manual rápido (ej. lock:event:105:seat:A1)
                String[] parts = expiredKey.split(":");
                if (parts.length == 5 && "event".equals(parts[1]) && "seat".equals(parts[3])) {
                    Long eventId = Long.parseLong(parts[2]);
                    String seatId = parts[4];
                    
                    // Al expirar, notificamos al frontend que el asiento está AVAILABLE de nuevo
                    SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "AVAILABLE", null);
                    messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
                    
                    logger.info("Broadcasted AVAILABLE state for seat {} (Event {}) due to TTL expiration", seatId, eventId);
                }
            } catch (Exception e) {
                logger.error("Error parsing expired key and broadcasting seat unlock: " + expiredKey, e);
            }
        }
    }
}
