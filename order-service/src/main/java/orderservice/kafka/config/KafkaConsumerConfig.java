package orderservice.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import orderservice.kafka.event.PaymentCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, PaymentCreatedEvent> paymentCreatedConsumerFactory(ObjectMapper mapper) {

        JsonDeserializer<PaymentCreatedEvent> deserializer =
                new JsonDeserializer<>(PaymentCreatedEvent.class, mapper);
        deserializer.addTrustedPackages("*");

        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "order-service",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class
        );

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent>
    paymentCreatedKafkaListenerFactory(
            ConsumerFactory<String, PaymentCreatedEvent> factory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent> listener =
                new ConcurrentKafkaListenerContainerFactory<>();

        listener.setConsumerFactory(factory);
        listener.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2000, 3)));

        return listener;
    }
}

