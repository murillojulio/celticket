package com.celticket.backend.dto;

import java.util.List;

public class WompiCheckoutSessionResponse {
    private String reference;
    private Integer amountInCents;
    private Integer subtotalCents;
    private Integer serviceFeeCents;
    private String currency;
    private String publicKey;
    private String integritySignature;
    private String redirectUrl;
    private String customerEmail;
    private String status;
    private List<CheckoutQuoteResponse.SeatPriceItem> items;

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Integer getAmountInCents() { return amountInCents; }
    public void setAmountInCents(Integer amountInCents) { this.amountInCents = amountInCents; }
    public Integer getSubtotalCents() { return subtotalCents; }
    public void setSubtotalCents(Integer subtotalCents) { this.subtotalCents = subtotalCents; }
    public Integer getServiceFeeCents() { return serviceFeeCents; }
    public void setServiceFeeCents(Integer serviceFeeCents) { this.serviceFeeCents = serviceFeeCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getIntegritySignature() { return integritySignature; }
    public void setIntegritySignature(String integritySignature) { this.integritySignature = integritySignature; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<CheckoutQuoteResponse.SeatPriceItem> getItems() { return items; }
    public void setItems(List<CheckoutQuoteResponse.SeatPriceItem> items) { this.items = items; }
}
