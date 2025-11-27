package orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import orderservice.kafka.event.PaymentCreatedEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentCreatedEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${topic.payment-created}", groupId = "order-service")
    public void handle(PaymentCreatedEvent event) {
        log.info("Received CREATE_PAYMENT event: {}", event);

        Long orderId = Long.valueOf(event.orderId());

        OrderResponse orderResponse = orderService.getOrderById(orderId);

        OrderRequest orderRequest = new OrderRequest(orderResponse.userResponse().id(), OrderStatus.PAYED, orderResponse.orderItems());

        orderService.updateOrder(orderId, orderRequest);
    }
}

