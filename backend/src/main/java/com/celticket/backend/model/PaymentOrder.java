package com.celticket.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String provider;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 1200)
    private String seatIdsJson;

    @Column(length = 4000)
    private String itemsJson;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Column(length = 255)
    private String buyerEmail;

    @Column(nullable = false)
    private Integer amountInCents;

    @Column(nullable = false)
    private Integer subtotalCents;

    @Column(nullable = false)
    private Integer serviceFeeCents;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 120)
    private String providerTransactionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getSeatIdsJson() { return seatIdsJson; }
    public void setSeatIdsJson(String seatIdsJson) { this.seatIdsJson = seatIdsJson; }
    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public Integer getAmountInCents() { return amountInCents; }
    public void setAmountInCents(Integer amountInCents) { this.amountInCents = amountInCents; }
    public Integer getSubtotalCents() { return subtotalCents; }
    public void setSubtotalCents(Integer subtotalCents) { this.subtotalCents = subtotalCents; }
    public Integer getServiceFeeCents() { return serviceFeeCents; }
    public void setServiceFeeCents(Integer serviceFeeCents) { this.serviceFeeCents = serviceFeeCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
