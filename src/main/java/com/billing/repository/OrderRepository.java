package com.billing.repository;

import com.billing.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByInvoiceNumber(String invoiceNumber);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.createdAt >= :startOfDay AND o.createdAt < :startOfNextDay AND o.paymentStatus = 'PAID'")
    BigDecimal getTodayRevenue(java.time.LocalDateTime startOfDay, java.time.LocalDateTime startOfNextDay);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay AND o.createdAt < :startOfNextDay")
    long getTodayBillCount(java.time.LocalDateTime startOfDay, java.time.LocalDateTime startOfNextDay);

    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), " +
           "COUNT(o), SUM(o.totalAmount), SUM(o.totalGst) " +
           "FROM Order o WHERE o.paymentStatus != 'CANCELLED' " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC")
    List<Object[]> getMonthlyReport();

    @Query("SELECT CAST(o.createdAt AS date), COUNT(o), SUM(o.totalAmount), SUM(o.totalGst) " +
           "FROM Order o WHERE o.paymentStatus != 'CANCELLED' " +
           "GROUP BY CAST(o.createdAt AS date) ORDER BY CAST(o.createdAt AS date) DESC")
    List<Object[]> getDailyReport();
}
