package paymentservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        List<OrderItemEvent> items,
        String status,
        LocalDateTime createdAt,
        BigDecimal paymentAmount
) {}

