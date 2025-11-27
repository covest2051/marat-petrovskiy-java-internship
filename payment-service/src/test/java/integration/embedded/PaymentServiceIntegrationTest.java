package integration.embedded;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import paymentservice.PaymentServiceApplication;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.kafka.event.OrderCreatedEvent;
import paymentservice.kafka.event.PaymentCreatedEvent;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, count = 3, controlledShutdown = true)
@SpringBootTest(classes = PaymentServiceApplication.class)
public class PaymentServiceIntegrationTest {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMessageListenerContainer<String, PaymentCreatedEvent> container;
    private BlockingQueue<ConsumerRecord<String, PaymentCreatedEvent>> records;

    @BeforeAll
    void setUp() {
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
        ContainerProperties containerProperties = new ContainerProperties("create-payment-event-topic");

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, PaymentCreatedEvent>) records::add);

        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterAll
    void tearDown() {
        container.stop();
    }

    @Test
    void testCreatePayment_shouldSavePayment() throws InterruptedException {
        PaymentRequest request = new PaymentRequest(3L, 3L, "PROCESSING", BigDecimal.valueOf(200));

        PaymentResponse response = paymentService.createPayment(request);

        ConsumerRecord<String, PaymentCreatedEvent> message = records.poll(3000, TimeUnit.MILLISECONDS);

        assertThat(message).isNotNull();
        assertThat(message.key()).isNotNull();

        assertThat(response).isNotNull();
        assertThat(response.paymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(paymentRepository.findById(response.id())).isPresent();
    }

    private Map<String, Object> getConsumerProperties() {
        JsonDeserializer<OrderCreatedEvent> deserializer = new JsonDeserializer<>(OrderCreatedEvent.class, objectMapper);
        deserializer.addTrustedPackages("*");

        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "payment-service",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer
        );
    }
}
