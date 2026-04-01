package com.celticket.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SectionDTO {
    private String id;
    private String name;
    private int rows;
    private int cols;
    private Double x;
    private Double y;
    private Integer basePrice;
    private Integer seatSize;
    private Integer seatGap;
    private List<Map<String, Object>> seats;

    public SectionDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    public Integer getBasePrice() { return basePrice; }
    public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }
    public Integer getSeatSize() { return seatSize; }
    public void setSeatSize(Integer seatSize) { this.seatSize = seatSize; }
    public Integer getSeatGap() { return seatGap; }
    public void setSeatGap(Integer seatGap) { this.seatGap = seatGap; }
    public List<Map<String, Object>> getSeats() { return seats; }
    public void setSeats(List<Map<String, Object>> seats) { this.seats = seats; }
}
