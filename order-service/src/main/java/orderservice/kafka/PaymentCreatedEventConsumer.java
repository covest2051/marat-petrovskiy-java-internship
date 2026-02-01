package orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderService;
import orderservice.service.impl.OrderServiceImpl;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import orderservice.kafka.event.PaymentCreatedEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentCreatedEventConsumer {

    private final OrderServiceImpl orderService;

    @KafkaListener(topics = "${topic.payment-created}", groupId = "order-service")
    public void handle(PaymentCreatedEvent event) {
        log.info("Received CREATE_PAYMENT event for userId: {}", event.userId());

        if (!"CREATED".equals(event.status())) {
            log.warn("Payment for order {} failed with status {}. Skipping status update.",
                    event.orderId(), event.status());
            return;
        }

        Long orderId = Long.valueOf(event.orderId());
        orderService.updateOrderStatus(orderId, OrderStatus.PAYED);
    }
}

