package orderservice.kafka.event;

public record OrderItemEvent(
        Long productId,
        Integer quantity
) {}

