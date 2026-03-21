package com.celticket.backend.service;

import com.celticket.backend.dto.WompiCheckoutSessionResponse;
import com.celticket.backend.model.PaymentOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Service
public class WompiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wompi.base-url:https://sandbox.wompi.co/v1}")
    private String wompiBaseUrl;

    @Value("${wompi.public-key:}")
    private String wompiPublicKey;

    @Value("${wompi.private-key:}")
    private String wompiPrivateKey;

    @Value("${wompi.integrity-secret:}")
    private String wompiIntegritySecret;

    @Value("${wompi.redirect-url:http://localhost:8080/payment-result}")
    private String wompiRedirectUrl;

    public WompiCheckoutSessionResponse buildCheckoutSession(PaymentOrder order) {
        WompiCheckoutSessionResponse response = new WompiCheckoutSessionResponse();
        response.setReference(order.getReference());
        response.setAmountInCents(order.getAmountInCents());
        response.setCurrency(order.getCurrency());
        response.setPublicKey(wompiPublicKey);
        response.setRedirectUrl(wompiRedirectUrl);
        response.setCustomerEmail(order.getBuyerEmail());
        response.setStatus(order.getStatus());
        response.setIntegritySignature(calculateIntegritySignature(
                order.getReference(),
                order.getAmountInCents(),
                order.getCurrency()
        ));
        return response;
    }

    public Map<String, Object> fetchTransactionById(String transactionId) {
        String url = wompiBaseUrl + "/transactions/" + transactionId;
        HttpHeaders headers = new HttpHeaders();
        if (wompiPrivateKey != null && !wompiPrivateKey.isBlank()) {
            headers.setBearerAuth(wompiPrivateKey);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public String calculateIntegritySignature(String reference, Integer amountInCents, String currency) {
        if (wompiIntegritySecret == null || wompiIntegritySecret.isBlank()) {
            return "";
        }
        String raw = reference + amountInCents + currency + wompiIntegritySecret;
        return sha256(raw);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular firma de integridad Wompi.", e);
        }
    }
}
