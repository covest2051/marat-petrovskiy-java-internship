package orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedEvent(
        String paymentId,
        String orderId,
        Long userId,
        String status,
        Instant timestamp,
        BigDecimal paymentAmount
) {
}

