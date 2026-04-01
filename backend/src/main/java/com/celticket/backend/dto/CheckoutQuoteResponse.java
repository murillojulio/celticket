package com.celticket.backend.dto;

import java.util.List;

public class CheckoutQuoteResponse {
    private String currency;
    private Integer subtotal;
    private Integer serviceFee;
    private Integer total;
    private List<SeatPriceItem> items;

    public static class SeatPriceItem {
        private String seatId;
        private String section;
        private Integer unitPrice;

        public SeatPriceItem() {}

        public SeatPriceItem(String seatId, String section, Integer unitPrice) {
            this.seatId = seatId;
            this.section = section;
            this.unitPrice = unitPrice;
        }

        public String getSeatId() { return seatId; }
        public void setSeatId(String seatId) { this.seatId = seatId; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public Integer getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
    public Integer getServiceFee() { return serviceFee; }
    public void setServiceFee(Integer serviceFee) { this.serviceFee = serviceFee; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public List<SeatPriceItem> getItems() { return items; }
    public void setItems(List<SeatPriceItem> items) { this.items = items; }
}
