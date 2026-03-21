package com.celticket.backend.repository;

import com.celticket.backend.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventId(Long eventId);
    List<Seat> findByEventIdAndStatus(Long eventId, String status);
    Optional<Seat> findByEventIdAndSeatCode(Long eventId, String seatCode);
    void deleteByEvent(com.celticket.backend.model.Event event);
}
