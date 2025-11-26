package orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.dto.mapper.OrderEventMapper;
import orderservice.entity.Order;
import orderservice.kafka.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    @Value("${topic.order-created}")
    private String orderCreatedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderEventMapper orderEventMapper;


    public void sendOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = orderEventMapper.toOrderCreatedEvent(order);

        try {
            kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.orderId()), event);
            log.info("Sent event to topic {} for userId={}, orderId={}", orderCreatedTopic, event.userId(), event.orderId());
        } catch (Exception e) {
            log.error("Failed to send event to topic {} for userId={}, orderId={}", orderCreatedTopic, event.userId(), event.orderId());
        }
    }
}