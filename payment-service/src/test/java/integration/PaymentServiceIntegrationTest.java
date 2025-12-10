package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import paymentservice.PaymentServiceApplication;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.kafka.event.OrderCreatedEvent;
import paymentservice.kafka.event.OrderItemEvent;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(classes = {PaymentServiceApplication.class})
class PaymentServiceIntegrationTest {

    @Value("${topic.order-created}")
    private String orderCreatedTopic;

    private KafkaTemplate<String, Object> kafkaTemplate;

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0.8")
            .withReuse(false);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    PaymentServiceIntegrationTest(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("topic.order-created", () -> "order-created");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = Map.of(
                "bootstrap.servers", kafka.getBootstrapServers(),
                "key.serializer", StringSerializer.class,
                "value.serializer", JsonSerializer.class
        );

        DefaultKafkaProducerFactory<String, Object> pf =
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new JsonSerializer<>(objectMapper));
        kafkaTemplate = new KafkaTemplate<>(pf);

        paymentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }

    private Payment payment(long orderId, long userId, PaymentStatus status, int secondsAgo, int amount) {
        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .timestamp(Instant.now().minusSeconds(secondsAgo))
                .paymentAmount(BigDecimal.valueOf(amount))
                .build();
    }

    @Test
    void shouldHandleCreateOrderEvent() {
        List<OrderItemEvent> items = List.of(
                new OrderItemEvent(100L, 2)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                1L,
                items,
                "CREATED",
                LocalDateTime.now(),
                BigDecimal.TEN
        );

        kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.orderId()), event);

        Pageable pageable = PageRequest.of(0, 10);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Page<Payment> payment = paymentRepository.findByOrderId(
                            1L, pageable
                    );
                    assertThat(payment.getContent()).isNotEmpty();
                    assertThat(payment.getContent().get(0).getUserId()).isEqualTo(1L);
                });
    }

    @Test
    void createPayment_shouldSavePayment() {
        PaymentRequest request = new PaymentRequest(1L, 1L, "CREATED", BigDecimal.TEN);
        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.paymentAmount()).isEqualByComparingTo(BigDecimal.TEN);

        Optional<Payment> payment = paymentRepository.findById(response.id());

        assertThat(payment).isPresent();
        assertThat(payment.get().getOrderId()).isEqualTo(1L);
    }

    @Test
    void getPaymentsByOrderId() {
        Payment p1 = payment(1, 1, PaymentStatus.CREATED, 3600, 10);
        Payment p2 = payment(1, 1, PaymentStatus.CREATED, 0, 20);
        Payment p3 = payment(2, 2, PaymentStatus.ERROR, 0, 30);

        paymentRepository.saveAll(List.of(p1, p2, p3));

        List<PaymentResponse> paymentResponseList = paymentService.getPaymentsByOrderId(0, 10, 1L);
        assertThat(paymentResponseList).hasSize(2);
        assertThat(paymentResponseList.get(0).orderId()).isEqualTo(1L);
    }

    @Test
    void getPaymentsByUserId() {
        Payment p1 = payment(1, 1, PaymentStatus.CREATED, 3600, 10);
        Payment p2 = payment(1, 1, PaymentStatus.CREATED, 0, 20);
        Payment p3 = payment(2, 2, PaymentStatus.ERROR, 0, 30);

        paymentRepository.saveAll(List.of(p1, p2, p3));

        List<PaymentResponse> byUser = paymentService.getPaymentsByUserId(0, 10, 1L);
        assertThat(byUser).hasSize(2);
    }

    @Test
    void getAllPaymentsByPeriod() {
        Payment p1 = payment(1, 1, PaymentStatus.CREATED, 3600, 10);
        Payment p2 = payment(1, 1, PaymentStatus.CREATED, 0, 20);
        Payment p3 = payment(2, 2, PaymentStatus.ERROR, 0, 30);

        paymentRepository.saveAll(List.of(p1, p2, p3));

        Instant from = Instant.now().minusSeconds(7200);
        Instant to = Instant.now().plusSeconds(60);

        BigDecimal totalAll = paymentService.getAllPaymentsByPeriod(0, 10, from, to);
        assertThat(totalAll).isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    @Test
    void getUserPaymentsByPeriod() {
        Payment p1 = payment(1, 1, PaymentStatus.CREATED, 3600, 10);
        Payment p2 = payment(1, 1, PaymentStatus.CREATED, 0, 20);
        Payment p3 = payment(2, 2, PaymentStatus.ERROR, 0, 30);

        paymentRepository.saveAll(List.of(p1, p2, p3));

        Instant from = Instant.now().minusSeconds(7200);
        Instant to = Instant.now().plusSeconds(60);

        BigDecimal totalUser300 = paymentService.getUserPaymentsByPeriod(0, 10, 1L, from, to);
        assertThat(totalUser300).isEqualByComparingTo(BigDecimal.valueOf(30));
    }
}
