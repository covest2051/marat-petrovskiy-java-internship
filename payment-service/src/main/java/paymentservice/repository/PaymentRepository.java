package paymentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Page<Payment> findByOrderId(Long orderId, Pageable pageable);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findByTimestampBetweenAndStatus(Instant from, Instant to, String status);

    @Query("{ 'userId' : ?0, 'timestamp' : { $gte: ?1, $lte: ?2 } }")
    List<Payment> findByUserIdAndTimestampBetweenAndStatus(Long userId, Instant from, Instant to, String status);

    List<Payment> findByOrderId(Long orderId);
}
