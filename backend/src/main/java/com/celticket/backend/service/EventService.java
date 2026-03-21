package com.celticket.backend.service;

import com.celticket.backend.dto.LayoutDTO;
import com.celticket.backend.dto.SectionDTO;
import com.celticket.backend.model.Event;
import com.celticket.backend.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public EventService(EventRepository eventRepository, ObjectMapper objectMapper, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateEventLayout(Long eventId, LayoutDTO layoutDTO) throws JsonProcessingException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        // 1. Guardar el JSON original en layoutConfig
        String layoutJson = objectMapper.writeValueAsString(layoutDTO);
        event.setLayoutConfig(layoutJson);
        event = eventRepository.save(event); // Re-asignar para asegurar estado persistido

        // 2. Limpiar sillas actuales
        jdbcTemplate.update("DELETE FROM seats WHERE event_id = ?", event.getId());

        // 3. Generar nuevas sillas basadas en las secciones
        if (layoutDTO.getSections() != null) {
            for (SectionDTO section : layoutDTO.getSections()) {
                if (section.getSeats() != null && !section.getSeats().isEmpty()) {
                    for (Map<String, Object> seatData : section.getSeats()) {
                        String row = (String) seatData.get("row");
                        Object number = seatData.get("number");
                        String seatCode = String.format("%s-%s%s", section.getName(), row, number);
                        
                        jdbcTemplate.update("INSERT INTO seats (seat_code, status, event_id) VALUES (?, ?, ?)", 
                                           seatCode, "AVAILABLE", event.getId());
                    }
                } else {
                    if (section.getRows() > 0 && section.getCols() > 0) {
                        for (int r = 0; r < section.getRows(); r++) {
                            char rowChar = (char) ('A' + r);
                            for (int c = 1; c <= section.getCols(); c++) {
                                String seatCode = String.format("%s-%s%d", section.getName(), rowChar, c);
                                
                                jdbcTemplate.update("INSERT INTO seats (seat_code, status, event_id) VALUES (?, ?, ?)", 
                                                   seatCode, "AVAILABLE", event.getId());
                            }
                        }
                    }
                }
            }
        }
    }
}
