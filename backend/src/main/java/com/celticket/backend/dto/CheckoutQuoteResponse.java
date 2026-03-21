package com.celticket.backend.dto;

import java.util.List;

public class CheckoutQuoteResponse {
    private String currency;
    private Integer subtotalCents;
    private Integer serviceFeeCents;
    private Integer totalCents;
    private List<SeatPriceItem> items;

    public static class SeatPriceItem {
        private String seatId;
        private String section;
        private Integer unitPriceCents;

        public SeatPriceItem() {}

        public SeatPriceItem(String seatId, String section, Integer unitPriceCents) {
            this.seatId = seatId;
            this.section = section;
            this.unitPriceCents = unitPriceCents;
        }

        public String getSeatId() { return seatId; }
        public void setSeatId(String seatId) { this.seatId = seatId; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public Integer getUnitPriceCents() { return unitPriceCents; }
        public void setUnitPriceCents(Integer unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getSubtotalCents() { return subtotalCents; }
    public void setSubtotalCents(Integer subtotalCents) { this.subtotalCents = subtotalCents; }
    public Integer getServiceFeeCents() { return serviceFeeCents; }
    public void setServiceFeeCents(Integer serviceFeeCents) { this.serviceFeeCents = serviceFeeCents; }
    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer totalCents) { this.totalCents = totalCents; }
    public List<SeatPriceItem> getItems() { return items; }
    public void setItems(List<SeatPriceItem> items) { this.items = items; }
}
