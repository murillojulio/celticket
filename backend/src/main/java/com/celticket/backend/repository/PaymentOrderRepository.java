package com.celticket.backend.repository;

import com.celticket.backend.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByReference(String reference);
    Optional<PaymentOrder> findByProviderTransactionId(String providerTransactionId);
}
