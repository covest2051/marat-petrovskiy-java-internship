package paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import paymentservice.dto.mapper.PaymentEventMapper;
import paymentservice.entity.Payment;
import paymentservice.kafka.event.PaymentCreatedEvent;

import java.util.concurrent.CompletableFuture;

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

        CompletableFuture<SendResult<String, Object>> futureResult =
                kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.orderId()), event);

        futureResult.whenComplete((result, ex) -> {
            if(ex == null) {
                log.info("Sent event to topic {} for userId={}, orderId={}." +
                                " Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(), event.userId(), event.orderId(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send event to topic {} for userId={}, orderId={}", orderCreatedTopic, event.userId(), event.orderId());
            }
        });
    }
}

