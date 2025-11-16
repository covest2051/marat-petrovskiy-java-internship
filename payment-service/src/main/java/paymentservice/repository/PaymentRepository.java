package paymentservice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId, Pageable pageable);

    List<Payment> findAllByUserId(Long userId, Pageable pageable);

    List<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(p.paymentAmount), 0)
           FROM Payment p
           WHERE p.timestamp BETWEEN :from AND :to
           """)
    BigDecimal getTotalSumForPeriod(Instant from, Instant to, Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(p.paymentAmount), 0)
           FROM Payment p
           WHERE p.userId = :userId
             AND p.timestamp BETWEEN :from AND :to
           """)
    BigDecimal getTotalSumForPeriodByUser(Long userId, Instant from, Instant to, Pageable pageable);
}
