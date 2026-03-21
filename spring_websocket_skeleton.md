# Esqueleto del Servidor WebSocket en Spring Boot + Redis

Para manejar conexiones masivas y el estado con Redis, Spring Boot ofrece soporte excelente a través de `spring-boot-starter-websocket` y `spring-boot-starter-data-redis`.

A continuación, tienes las 4 clases principales que forman la columna vertebral de esta solución.

## 1. Dependencias (pom.xml)
Asegúrate de tener estas dependencias en tu proyecto Spring Boot:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Opcional: Jackson para parseo rápido JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

---

## 2. Configuración del WebSocket (WebSocketConfig.java)
Esta clase habilita el broker de mensajes STOMP sobre WebSockets (el estándar más usado en Spring).

```java
package com.celticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un broker en memoria "simple" para enviar mensajes a los clientes
        // Los clientes se suscribirán a rutas que comiencen por "/topic" o "/queue"
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para los mensajes que envían los clientes hacia el servidor (ej: @MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // El endpoint principal de conexión WS. 
        // Con SockJS se permite fallback en caso de que WS no esté soportado por el proxy o red.
        registry.addEndpoint("/seats-ws")
                .setAllowedOriginPatterns("*") // En producción cambia esto a tu dominio real
                .withSockJS(); 
    }
}
```

---

## 3. Modelos de Mensajes DTO (SeatMessage.java)
Los objetos de transferencia para mapear los JSON que entran y salen.

```java
package com.celticket.dto;

public class SeatMessage {
    private String action;   // JOIN_ROOM, SEAT_LOCK_REQUEST, SEAT_UNLOCK_REQUEST
    private Long eventId;
    private String seatId;
    private String userId;
    private String state;    // LOCKED, AVAILABLE, SOLD
    
    // Contructores, Getters y Setters...
    
    public SeatMessage() {}

    public SeatMessage(Long eventId, String seatId, String state, String userId) {
        this.eventId = eventId;
        this.seatId = seatId;
        this.state = state;
        this.userId = userId;
    }
    
    // ... Agregar todos los getters y setters aquí
}
```

---

## 4. El Controlador WebSocket (SeatWebSocketController.java)
Recibe las peticiones del cliente (ej. `SEAT_LOCK_REQUEST`) y las despacha al servicio. Utiliza `@MessageMapping`.

```java
package com.celticket.controller;

import com.celticket.dto.SeatMessage;
import com.celticket.service.SeatLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SeatWebSocketController {

    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public SeatWebSocketController(SeatLockService seatLockService, SimpMessagingTemplate messagingTemplate) {
        this.seatLockService = seatLockService;
        this.messagingTemplate = messagingTemplate;
    }

    // El cliente envía a: /app/seat.lock.{eventId}
    @MessageMapping("/seat.lock.{eventId}")
    public void handleLockRequest(@DestinationVariable Long eventId, SeatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        // En un caso real, el userId vendría del token JWT (Principal) en el Header, 
        // aquí lo tomamos del mensaje por propósitos del mock.
        String userId = message.getUserId();
        String seatId = message.getSeatId();

        boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);

        if (locked) {
            // El lock fue exitoso en Redis. Hacemos broadcast a todos los que miran este evento.
            // Para suscribirse, el Frontend debe escuchar en: /topic/event.105
            SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "LOCKED", userId);
            messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
            
            // Opcional: Responderle SOLO al usuario confirmando el éxito a su cola privada ("user queue")
        } else {
            // Avisarle temporalmente al usuario que falló por concurrencia
            // messagingTemplate.convertAndSendToUser(...)
        }
    }

    // El cliente envía a: /app/seat.unlock.{eventId}
    @MessageMapping("/seat.unlock.{eventId}")
    public void handleUnlockRequest(@DestinationVariable Long eventId, SeatMessage message) {
        String userId = message.getUserId();
        String seatId = message.getSeatId();

        boolean unlocked = seatLockService.unlockSeat(eventId, seatId, userId);

        if (unlocked) {
            // Hacemos broadcast diciendo que la silla volvió a estar disponible
            SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "AVAILABLE", null);
            messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
        }
    }
}
```

---

## 5. El Servicio de Redis (SeatLockService.java)
Aquí es donde ocurre la magia de la concurrencia. Usamos `StringRedisTemplate` para interactuar con Redis enviando comandos SETNX.

```java
package com.celticket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    
    // Tiempo límite que una silla permanece reservada sin pagarse (10 minutos)
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);

    @Autowired
    public SeatLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Intenta bloquear una silla de forma atómica en Redis.
     */
    public boolean tryLockSeat(Long eventId, String seatId, String userId) {
        String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
        
        // El método setIfAbsent ejecuta "SET key value NX" (Only set if Not EXists)
        // Devuelve TRUE si la clave se creó (nadie más la tenía), FALSE si ya existía.
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, userId, LOCK_TIMEOUT);
        
        return Boolean.TRUE.equals(success);
    }

    /**
     * Libera (desbloquea) una silla voluntariamente.
     */
    public boolean unlockSeat(Long eventId, String seatId, String userId) {
        String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
        
        // Verificamos quién es el dueño actual del lock en Redis
        String currentOwner = redisTemplate.opsForValue().get(key);
        
        // Solo puede desbloquearla quien la bloqueó (o un administrador)
        if (userId.equals(currentOwner)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
    
    /**
     * Este método se llamaría cuando el pago en Wompi sea exitoso,
     * para cambiar el estado de la silla a vendido (persistiéndolo en SQL).
     */
    public void confirmPurchase(Long eventId, String seatId, String userId) {
        // 1. Validar final en BD Relacional (MySQL/PostgreSQL)
        // 2. Insertar en tabla EventSeat status = 'SOLD'
        // 3. Eliminar el Lock efímero de Redis
        String key = String.format("lock:event:%d:seat:%s", eventId, seatId);
        redisTemplate.delete(key);
        
        // Aquí desencadenarías el broadcast de "SOLD" mediante SimpMessagingTemplate
    }
}
```

## Resumen de la Integración Frontend <-> Spring

Basado en el uso de **STOMP** (Simple Text Oriented Messaging Protocol, que Spring Boot soporta nativamente sobre WebSockets):

1. Tu **Frontend JS** se conectará usando las librerías `sockjs-client` y `stompjs`.
   ```javascript
   let socket = new SockJS('http://localhost:8080/seats-ws');
   let stompClient = Stomp.over(socket);
   
   stompClient.connect({}, function (frame) {
       // El cliente se SUBSCRIBE para recibir cambios instantáneos ("Broadcasts")
       stompClient.subscribe('/topic/event.105', function (response) {
           let msg = JSON.parse(response.body);
           updateSeatVisual(msg.seatId, msg.state);
       });
   });
   ```

2. Tu **Frontend JS** enviará comandos (Locks) al servidor así:
   ```javascript
   let payload = { eventId: 105, seatId: 'C4', userId: 'user99' };
   stompClient.send("/app/seat.lock.105", {}, JSON.stringify(payload));
   ```
