package com.celticket.backend.controller;

import com.celticket.backend.model.Event;
import com.celticket.backend.model.Seat;
import com.celticket.backend.repository.EventRepository;
import com.celticket.backend.repository.SeatRepository;
import com.celticket.backend.dto.LayoutDTO;
import com.celticket.backend.service.EventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventService eventService;

    public EventController(EventRepository eventRepository, SeatRepository seatRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<Seat>> getSeatsByEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(seatRepository.findByEventId(id));
    }

    @PutMapping(value = "/{id}/layout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateLayout(@PathVariable Long id, @RequestBody LayoutDTO layoutDTO) 
                             throws JsonProcessingException {
        eventService.updateEventLayout(id, layoutDTO);
    }
}
