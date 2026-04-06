package com.celticket.backend.repository;

import com.celticket.backend.model.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByReference(String reference);
    Optional<PaymentOrder> findByProviderTransactionId(String providerTransactionId);

    long countByEventId(Long eventId);

    @Query("""
            SELECT p FROM PaymentOrder p
            WHERE (:eventId IS NULL OR p.eventId = :eventId)
            AND (:status IS NULL OR p.status = :status)
            """)
    Page<PaymentOrder> search(@Param("eventId") Long eventId,
                                @Param("status") String status,
                                Pageable pageable);
}
