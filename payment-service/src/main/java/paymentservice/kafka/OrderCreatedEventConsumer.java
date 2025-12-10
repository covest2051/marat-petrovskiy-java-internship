package paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import paymentservice.kafka.event.OrderCreatedEvent;
import paymentservice.service.PaymentService;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "${topic.order-created}", groupId = "payment-service")
    public void handle(OrderCreatedEvent event) {
        log.info("Received CREATE_ORDER event: {}", event);

        paymentService.createPaymentFromOrder(event);
    }
}

