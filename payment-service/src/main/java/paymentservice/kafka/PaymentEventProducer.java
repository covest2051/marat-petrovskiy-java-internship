package paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import paymentservice.dto.mapper.PaymentEventMapper;
import paymentservice.entity.Payment;
import paymentservice.kafka.event.PaymentCreatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    @Value("${topic.payment-created}")
    private String orderCreatedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentEventMapper paymentEventMapper;

    public void sendPaymentCreatedEvent(Payment payment) {
        PaymentCreatedEvent event = paymentEventMapper.toPaymentCreatedEvent(payment);

        try {
            kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.orderId()), event);
            log.info("Sent event to topic {} for userId={}, orderId={}", orderCreatedTopic, event.userId(), event.orderId());
        } catch (Exception e) {
            log.error("Failed to send event to topic {} for userId={}, orderId={}", orderCreatedTopic, event.userId(), event.orderId());
        }
    }
}

