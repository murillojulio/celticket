package com.celticket.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;

    private String buyerEmail;
    private String sessionId;
    private LocalDateTime purchaseDate;

    public Ticket() {}

    public Ticket(Seat seat, String buyerEmail, String sessionId, LocalDateTime purchaseDate) {
        this.seat = seat;
        this.buyerEmail = buyerEmail;
        this.sessionId = sessionId;
        this.purchaseDate = purchaseDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }
}
