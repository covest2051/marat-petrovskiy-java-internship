package paymentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, Long> {
    Page<Payment> findByOrderId(Long orderId, Pageable pageable);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findByTimestampBetween(Instant from, Instant to);

    List<Payment> findByUserIdAndTimestampBetween(Long userId, Instant from, Instant to);
}
