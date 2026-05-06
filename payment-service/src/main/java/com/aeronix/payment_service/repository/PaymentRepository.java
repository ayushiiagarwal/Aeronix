package com.aeronix.payment_service.repository;

import com.aeronix.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByBookingId(String bookingId);

    List<Payment> findByUserId(Integer userId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId " +
            "AND p.status = 'PAID'")
    Double sumAmountByUserId(@Param("userId") Integer userId);

    @Query("SELECT p FROM Payment p WHERE p.paidAt BETWEEN :start AND :end " +
            "AND p.status = 'PAID'")
    List<Payment> findByPaidAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paidAt BETWEEN :start AND :end " +
            "AND p.status = 'PAID'")
    Double sumRevenueBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    Double sumTotalRevenue();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PAID'")
    long countSuccessfulPayments();

    List<Payment> findByUserIdOrderByCreatedAtDesc(Integer userId);
}