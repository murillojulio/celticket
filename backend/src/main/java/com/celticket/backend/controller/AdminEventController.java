package com.celticket.backend.controller;

import com.celticket.backend.dto.AdminEventCreateRequest;
import com.celticket.backend.dto.AdminEventUpdateRequest;
import com.celticket.backend.dto.LayoutDTO;
import com.celticket.backend.model.Event;
import com.celticket.backend.service.AdminEventService;
import com.celticket.backend.service.EventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/events")
@CrossOrigin(origins = "*")
public class AdminEventController {

    private final AdminEventService adminEventService;
    private final EventService eventService;

    public AdminEventController(AdminEventService adminEventService, EventService eventService) {
        this.adminEventService = adminEventService;
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> list() {
        return adminEventService.listAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AdminEventCreateRequest request) {
        try {
            return ResponseEntity.ok(adminEventService.create(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AdminEventUpdateRequest request) {
        try {
            return ResponseEntity.ok(adminEventService.update(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            adminEventService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}/layout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateLayout(@PathVariable Long id, @RequestBody LayoutDTO layoutDTO)
            throws JsonProcessingException {
        eventService.updateEventLayout(id, layoutDTO);
        return ResponseEntity.noContent().build();
    }
}
