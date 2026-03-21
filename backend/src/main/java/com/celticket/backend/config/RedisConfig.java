package com.celticket.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Properties;

@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory, RedisExpirationListener expirationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Escucha eventos de expiración en cualquier base de datos (por defecto '0' si no está configurada y la expresión asume DB 0: __keyevent@0__:expired)
        container.addMessageListener(expirationListener, new PatternTopic("__keyevent@*__:expired"));
        
        return container;
    }

    // Asegurarse de que Keyspace Notifications ("Ex") esté activado en Redis
    @EventListener(ContextRefreshedEvent.class)
    public void enableRedisKeyspaceNotifications() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.info("server");
            
            // "Ex" = E: keyevent events, x: Expired events
            connection.setConfig("notify-keyspace-events", "Ex");
            logger.info("Successfully enabled Redis Keyspace Notifications (Config: Ex)");
            connection.close();
        } catch (Exception e) {
            logger.error("Failed to set notify-keyspace-events programmatically. Make sure Redis has 'notify-keyspace-events Ex' configured manually", e);
        }
    }
}
