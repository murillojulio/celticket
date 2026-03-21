package com.celticket.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "event_date")
    private LocalDateTime eventDate;
    @Column(name = "venue")
    private String venue;
    @Column(name = "row_count")
    private Integer rowCount;
    @Column(name = "column_count")
    private Integer columnCount;

    @Column(name = "layout_config", columnDefinition = "TEXT")
    private String layoutConfig;

    public Event() {}

    public Event(String name, LocalDateTime eventDate, String venue, Integer rowCount, Integer columnCount) {
        this.name = name;
        this.eventDate = eventDate;
        this.venue = venue;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    public Event(String name, LocalDateTime eventDate, String venue, String layoutConfig) {
        this.name = name;
        this.eventDate = eventDate;
        this.venue = venue;
        this.layoutConfig = layoutConfig;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public Integer getColumnCount() { return columnCount; }
    public void setColumnCount(Integer columnCount) { this.columnCount = columnCount; }

    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
}
