package com.celticket.backend.dto;

public class SeatMessage {
    private String action;   // JOIN_ROOM, SEAT_LOCK_REQUEST, SEAT_UNLOCK_REQUEST
    private Long eventId;
    private String seatId;
    private String userId;
    private String state;    // LOCKED, AVAILABLE, SOLD

    public SeatMessage() {
    }

    public SeatMessage(Long eventId, String seatId, String state, String userId) {
        this.eventId = eventId;
        this.seatId = seatId;
        this.state = state;
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "SeatMessage{" +
                "action='" + action + '\'' +
                ", eventId=" + eventId +
                ", seatId='" + seatId + '\'' +
                ", userId='" + userId + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
