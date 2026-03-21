package com.celticket.backend.config;

import com.celticket.backend.model.Event;
import com.celticket.backend.model.Seat;
import com.celticket.backend.repository.EventRepository;
import com.celticket.backend.repository.SeatRepository;
import com.celticket.backend.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataInitializer(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    CommandLineRunner initData(EventRepository eventRepository, SeatRepository seatRepository, TicketRepository ticketRepository) {
        return args -> {
            // Limpiar datos previos
            ticketRepository.deleteAll();
            jdbcTemplate.update("DELETE FROM seats");
            eventRepository.deleteAll();

            // Evento 1: Grande (10x10)
            Event e1 = new Event("Concierto Tech", LocalDateTime.now().plusDays(10), "Estadio Virtual", 10, 10);
            e1 = eventRepository.save(e1);
            createSeatsForEvent(e1);

            // Evento 2: Pequeño (5x4)
            Event e2 = new Event("Teatro Acústico", LocalDateTime.now().plusDays(5), "Sala VIP", 5, 4);
            e2 = eventRepository.save(e2);
            createSeatsForEvent(e2);
            
            System.out.println("Data inicial dinámica cargada correctamente (Hybrid JPA/JDBC).");
        };
    }

    private void createSeatsForEvent(Event event) {
        for (int r = 0; r < event.getRowCount(); r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 1; c <= event.getColumnCount(); c++) {
                String seatCode = rowChar + String.valueOf(c);
                jdbcTemplate.update("INSERT INTO seats (seat_code, status, event_id) VALUES (?, ?, ?)", 
                                   seatCode, "AVAILABLE", event.getId());
            }
        }
    }
}
