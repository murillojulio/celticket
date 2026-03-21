# Validación JWT en Spring Boot WebSockets

A diferencia de las peticiones REST normales (donde puedes enviar el token en la cabecera HTTP `Authorization`), los WebSockets en el navegador (especialmente con STOMP/SockJS) no siempre permiten enviar cabeceras HTTP personalizadas estables durante el proceso inicial de "handshake".

Para solucionar esto, el estándar en Spring es que el cliente envíe su JWT en las **cabeceras del mensaje CONNECT de STOMP**, y Spring lo intercepte antes de enrutar el mensaje.

Aquí tienes el esqueleto de cómo implementarlo:

## 1. Modificar WebSocketConfig.java

Debemos registrar un `ChannelInterceptor` en el canal de entrada (`clientInboundChannel`). Este interceptor atrapará el mensaje STOMP `CONNECT`, extraerá el token, lo validará, construirá un objeto "Authentication" y lo añadirá al contexto del socket.

```java
package com.celticket.config;

import com.celticket.security.JwtService; // Tu clase que valida y parsea el JWT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // IMPORTANTE: Se debe ejecutar ANTES de que Spring enrute el mensaje
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtService jwtService; // Tu servicio utility para manejar el token

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/seats-ws").setAllowedOriginPatterns("*").withSockJS();
    }

    // AQUI ESTÁ LA MAGIA PARA JWT EN WEBSOCKETS
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                // Si el mensaje es el de conexión inicial
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    
                    // Extraer los headers. El cliente JS mandará el token en el header "Authorization" o "Token"
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    
                    if (authorization != null && !authorization.isEmpty()) {
                        String token = authorization.get(0).replace("Bearer ", "");
                        
                        // 1. Validar el token usando tu lógica de seguridad
                        if (jwtService.isTokenValid(token)) {
                            // 2. Extraer el usuario (ej: extraer el username o id del payload del JWT)
                            String username = jwtService.extractUsername(token);
                            
                            // 3. Crear el objeto Authentication de Spring Security
                            // Aquí asumo que devuelves roles (null en este ejemplo)
                            Authentication userAuth = new UsernamePasswordAuthenticationToken(username, null, null); 
                            
                            // 4. Asignar este usuario a la sesión WebSocket!
                            // De esta manera, Spring sabrá quién es el dueño original de este socket
                            accessor.setUser(userAuth);
                        } else {
                            // Si el token es inválido, podrías lanzar una excepción o simplemente no definir setUser.
                            // Si no defines setUser y la ruta requiere autenticación, Spring cerrará el socket.
                            throw new IllegalArgumentException("Token JWT inválido en WebSocket Handshake");
                        }
                    } else {
                        throw new IllegalArgumentException("No se proporcionó Header de Autorización en el WS");
                    }
                }
                
                return message;
            }
        });
    }
}
```

## 2. Simplificar el Controlador (SeatWebSocketController)

Gracias a que configuramos el interceptor en el paso anterior, ahora tu controlador es mucho más fácil y seguro de usar.

Ya no necesitas confiar en un campo `userId` dentro del JSON (que cualquier atacante podría falsificar). En su lugar, obtienes el usuario que Spring Security inyectó a través del Socket Seguro Principal: `java.security.Principal`.

```java
import java.security.Principal; // El objeto nativo de Java/Spring para identidad

@Controller
public class SeatWebSocketController {

    @Autowired
    private SeatLockService seatLockService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/seat.lock.{eventId}")
    public void handleLockRequest(@DestinationVariable Long eventId, 
                                  SeatMessage message, 
                                  Principal principal) { // <-- ¡Spring inyecta la identidad conectada y validada!
                                  
        if (principal == null) {
            // El socket no está autenticado
            return; 
        }

        // Este nombre de usuario es 100% confiable, extraído del JWT.
        String secureUserId = principal.getName(); 
        String seatId = message.getSeatId();

        boolean locked = seatLockService.tryLockSeat(eventId, seatId, secureUserId);

        if (locked) {
            SeatMessage broadcastMsg = new SeatMessage(eventId, seatId, "LOCKED", secureUserId);
            messagingTemplate.convertAndSend("/topic/event." + eventId, broadcastMsg);
        }
    }
}
```

## 3. ¿Cómo enviarlo desde el Frontend (JS)?

Del lado del HTML/JS, a la hora de conectarse (el bloque que vimos antes), en el comando `stompClient.connect` debes mandar el diccionario de cabeceras (`headers`), el cual incluye tu Autorización:

```javascript
let socket = new SockJS('http://localhost:8080/seats-ws');
let stompClient = Stomp.over(socket);

// El JSON de cabeceras. 
// Aquí metes de manera estática (o dinámica extraída del localStorage) tu JWT "Bearer"
let stompHeaders = {
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwib...'
};

// Pasas las cabeceras como primer parámetro al conectar
stompClient.connect(stompHeaders, function (frame) {
    console.log("Conectado! Autenticado correctamente.");
    
    // Y ya te puedes subscribir
    stompClient.subscribe('/topic/event.105', function (response) {
       // ... lógicas visuales de los asientos.
    });
}, function(error) {
    console.error("No te pudiste conectar (quizá el JWT caducó)");
});
```
