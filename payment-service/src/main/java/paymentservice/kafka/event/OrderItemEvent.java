package paymentservice.kafka.event;

public record OrderItemEvent(
        Long productId,
        Integer quantity
) {}
