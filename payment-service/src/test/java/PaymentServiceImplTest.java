import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.dto.mapper.PaymentMapper;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.kafka.PaymentEventProducer;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private paymentservice.client.RandomNumberClient randomNumberClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPayment_shouldReturnPaymentResponse() {
        PaymentRequest request = new PaymentRequest(1L, 1L, "PENDING", BigDecimal.valueOf(100));
        Payment payment = Payment.builder()
                .orderId(1L)
                .userId(1L)
                .status(PaymentStatus.CREATED)
                .timestamp(Instant.now())
                .paymentAmount(BigDecimal.valueOf(100))
                .build();

        PaymentResponse response = new PaymentResponse(1L, 1L, 1L, PaymentStatus.CREATED, Instant.now(), BigDecimal.valueOf(100));

        when(randomNumberClient.getRandomNumber()).thenReturn(2); // чёт
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertEquals(PaymentStatus.CREATED, result.status());
        verify(paymentEventProducer).sendPaymentCreatedEvent(any(Payment.class));
    }

    @Test
    void createPayment_shouldGetErrorStatus() {
        PaymentRequest request = new PaymentRequest(1L, 1L, "PENDING", BigDecimal.valueOf(50));
        Payment payment = Payment.builder()
                .orderId(1L)
                .userId(1L)
                .status(PaymentStatus.ERROR)
                .timestamp(Instant.now())
                .paymentAmount(BigDecimal.valueOf(50))
                .build();

        PaymentResponse response = new PaymentResponse(1L, 1L, 1L, PaymentStatus.ERROR, Instant.now(), BigDecimal.valueOf(50));

        when(randomNumberClient.getRandomNumber()).thenReturn(1); // нечт
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertEquals(PaymentStatus.ERROR, result.status());
        verify(paymentEventProducer).sendPaymentCreatedEvent(any(Payment.class));
    }

    @Test
    void getPaymentsByOrderId_shouldReturnPayments() {
        Payment payment = new Payment();
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByOrderId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        when(paymentMapper.toPaymentResponseList(any()))
                .thenReturn(List.of());

        List<PaymentResponse> result = paymentService.getPaymentsByOrderId(0, 10, 1L);

        assertNotNull(result);
        verify(paymentRepository).findByOrderId(eq(1L), any(Pageable.class));
        verify(paymentMapper).toPaymentResponseList(any());
    }

    @Test
    void getPaymentsByUserId_shouldReturnPayments() {
        Payment payment = new Payment();
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findAllByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        when(paymentMapper.toPaymentResponseList(any()))
                .thenReturn(List.of());

        List<PaymentResponse> result = paymentService.getPaymentsByUserId(0, 10, 1L);

        assertNotNull(result);
        verify(paymentRepository).findAllByUserId(eq(1L), any(Pageable.class));
        verify(paymentMapper).toPaymentResponseList(any());
    }

    @Test
    void getPaymentsByStatus_shouldReturnPayments() {
        Payment payment = new Payment();
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findAllByStatus(eq(PaymentStatus.COMPLETE), any(Pageable.class)))
                .thenReturn(page);

        when(paymentMapper.toPaymentResponseList(any()))
                .thenReturn(List.of());

        List<PaymentResponse> result = paymentService.getPaymentsByStatus(0, 10, PaymentStatus.COMPLETE);

        assertNotNull(result);
        verify(paymentRepository).findAllByStatus(eq(PaymentStatus.COMPLETE), any(Pageable.class));
        verify(paymentMapper).toPaymentResponseList(any());
    }

    @Test
    void getAllPaymentsByPeriod_calculatesCorrectSum() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();

        List<Payment> payments = List.of(
                Payment.builder().paymentAmount(BigDecimal.valueOf(50)).build(),
                Payment.builder().paymentAmount(BigDecimal.valueOf(70)).build(),
                Payment.builder().paymentAmount(BigDecimal.valueOf(30)).build()
        );

        when(paymentRepository.findByTimestampBetween(from, to)).thenReturn(payments);

        BigDecimal result = paymentService.getAllPaymentsByPeriod(0, 10, from, to);

        assertEquals(BigDecimal.valueOf(150), result);
        verify(paymentRepository).findByTimestampBetween(from, to);
    }

    @Test
    void getUserPaymentsByPeriod_calculatesCorrectSum() {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Long userId = 1L;

        List<Payment> payments = List.of(
                Payment.builder().paymentAmount(BigDecimal.valueOf(40)).build(),
                Payment.builder().paymentAmount(BigDecimal.valueOf(27.89)).build()
        );

        when(paymentRepository.findByUserIdAndTimestampBetween(userId, from, to))
                .thenReturn(payments);

        BigDecimal result = paymentService.getUserPaymentsByPeriod(0, 10, userId, from, to);

        assertEquals(BigDecimal.valueOf(67.89), result);
        verify(paymentRepository).findByUserIdAndTimestampBetween(userId, from, to);
    }
}
