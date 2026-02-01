package integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;
import paymentservice.PaymentServiceApplication;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = PaymentServiceApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PaymentServiceIntegrationTest {

    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.10")
    );

    static {
        kafka.start();
        mongoDBContainer.start();
    }

    @org.springframework.test.context.DynamicPropertySource
    static void setProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    static WireMockServer wireMockServer = new WireMockServer(8089);


    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        paymentRepository.saveAll(List.of(
                new Payment(null, 1L, 1L, PaymentStatus.COMPLETE, Instant.now().minus(1, ChronoUnit.DAYS), BigDecimal.valueOf(50)),
                new Payment(null, 1L, 1L, PaymentStatus.COMPLETE, Instant.now().minus(2, ChronoUnit.DAYS), BigDecimal.valueOf(70)),
                new Payment(null, 2L, 2L, PaymentStatus.COMPLETE, Instant.now().minus(3, ChronoUnit.DAYS), BigDecimal.valueOf(30))
        ));
    }

//    @Test
//    void getUserPaymentsByPeriod_shouldReturnCorrectSum() {
//        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
//        Instant to = Instant.now();
//        Long userId = 1L;
//
//        BigDecimal total = paymentService.getUserPaymentsByPeriod(0, 10, userId, from, to);
//
//        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(120));
//    }

    @Test
    void createPayment_shouldSavePayment() {
        PaymentRequest request = new PaymentRequest(3L, 3L, "PROCESSING", BigDecimal.valueOf(200));

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.paymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(paymentRepository.findById(response.id())).isPresent();
    }
}
