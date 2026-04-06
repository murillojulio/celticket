package com.celticket.backend.service;

import com.celticket.backend.dto.AdminEventCreateRequest;
import com.celticket.backend.dto.AdminEventUpdateRequest;
import com.celticket.backend.model.Event;
import com.celticket.backend.repository.EventRepository;
import com.celticket.backend.repository.PaymentOrderRepository;
import com.celticket.backend.repository.SeatRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminEventService {

    private final EventRepository eventRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final EventService eventService;
    private final SeatRepository seatRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminEventService(EventRepository eventRepository,
                             PaymentOrderRepository paymentOrderRepository,
                             EventService eventService,
                             SeatRepository seatRepository,
                             JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.eventService = eventService;
        this.seatRepository = seatRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Event> listAll() {
        return eventRepository.findAll();
    }

    @Transactional
    public Event create(AdminEventCreateRequest request) {
        if (request.getRowCount() == null || request.getColumnCount() == null
                || request.getRowCount() <= 0 || request.getColumnCount() <= 0) {
            throw new IllegalArgumentException("rowCount y columnCount deben ser positivos.");
        }
        Event event = new Event(
                request.getName(),
                request.getEventDate(),
                request.getVenue(),
                request.getRowCount(),
                request.getColumnCount());
        event = eventRepository.save(event);
        eventService.seedGridSeats(event);
        return eventRepository.findById(event.getId()).orElse(event);
    }

    @Transactional
    public Event update(Long id, AdminEventUpdateRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + id));

        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
        }

        boolean gridChange = request.getRowCount() != null || request.getColumnCount() != null;
        if (gridChange) {
            int newRows = request.getRowCount() != null ? request.getRowCount() : event.getRowCount();
            int newCols = request.getColumnCount() != null ? request.getColumnCount() : event.getColumnCount();
            if (newRows <= 0 || newCols <= 0) {
                throw new IllegalArgumentException("rowCount y columnCount deben ser positivos.");
            }
            if (seatRepository.findByEventIdAndStatus(id, "SOLD").size() > 0) {
                throw new IllegalStateException("No se puede cambiar la rejilla: hay asientos vendidos.");
            }
            event.setRowCount(newRows);
            event.setColumnCount(newCols);
            jdbcTemplate.update("DELETE FROM tickets WHERE seat_id IN (SELECT id FROM seats WHERE event_id = ?)", id);
            jdbcTemplate.update("DELETE FROM seats WHERE event_id = ?", id);
            event = eventRepository.save(event);
            eventService.seedGridSeats(event);
        } else {
            event = eventRepository.save(event);
        }

        return event;
    }

    @Transactional
    public void delete(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Evento no encontrado: " + id);
        }
        if (paymentOrderRepository.countByEventId(id) > 0) {
            throw new IllegalStateException("No se puede eliminar: hay pedidos asociados a este evento.");
        }
        jdbcTemplate.update("DELETE FROM tickets WHERE seat_id IN (SELECT id FROM seats WHERE event_id = ?)", id);
        jdbcTemplate.update("DELETE FROM seats WHERE event_id = ?", id);
        eventRepository.deleteById(id);
    }
}
