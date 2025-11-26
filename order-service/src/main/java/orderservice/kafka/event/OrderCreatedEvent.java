package orderservice.kafka.event;

import orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        OrderStatus status,
        LocalDateTime creationDate,
        List<OrderItemEvent> orderItems,
        BigDecimal paymentAmount
) {
}
