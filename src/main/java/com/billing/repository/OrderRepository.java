package com.billing.repository;

import com.billing.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByInvoiceNumber(String invoiceNumber);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    // ── Dashboard ──────────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.createdAt >= :start AND o.createdAt < :end " +
           "AND o.paymentStatus = :paid")
    BigDecimal getTodayRevenue(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("paid")  Order.PaymentStatus paid);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long getTodayBillCount(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end);

    // ── Reports — using ORDER BY createdAt for grouping compatibility ─────

    @Query("SELECT o FROM Order o WHERE o.paymentStatus <> :cancelled ORDER BY o.createdAt DESC")
    List<Order> findNonCancelledOrders(@Param("cancelled") Order.PaymentStatus cancelled);
}
