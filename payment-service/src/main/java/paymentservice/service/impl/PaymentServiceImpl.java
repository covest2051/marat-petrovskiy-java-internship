package paymentservice.service.impl;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paymentservice.client.RandomNumberClient;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.dto.mapper.PaymentMapper;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.kafka.PaymentEventProducer;
import paymentservice.kafka.event.OrderCreatedEvent;
import paymentservice.kafka.event.PaymentCreatedEvent;
import paymentservice.metrics.PaymentMetrics;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentMetrics paymentMetrics;

    @Override
    @Transactional
    @Observed(name = "paymentservice.payments.create", contextualName = "create-payment")
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        log.info("Создание платежа orderId={} userId={} amount={}",
                paymentRequest.orderId(), paymentRequest.userId(), paymentRequest.paymentAmount());

        Optional<Payment> existing = paymentRepository.findByOrderId(paymentRequest.orderId())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREATED)
                .findFirst();

        if (existing.isPresent()) {
            paymentMetrics.incrementDuplicateSkipped();
            log.warn("Дубликат платежа orderId={} — возвращаем существующий id={}",
                    paymentRequest.orderId(), existing.get().getId());
            return paymentMapper.toPaymentResponse(existing.get());
        }

        Payment payment = buildPayment(
                paymentRequest.orderId(),
                paymentRequest.userId(),
                paymentRequest.paymentAmount()
        );

        Payment saved = paymentRepository.save(payment);
        recordOutcome(saved);

        paymentEventProducer.sendPaymentCreatedEvent(saved);
        paymentMetrics.incrementEventSent();

        log.info("Платёж создан id={} orderId={} статус={}",
                saved.getId(), saved.getOrderId(), saved.getStatus());
        return paymentMapper.toPaymentResponse(saved);
    }

    @Override
    @Transactional
    @Observed(name = "paymentservice.payments.create-from-order", contextualName = "create-payment-from-order")
    public PaymentResponse createPaymentFromOrder(OrderCreatedEvent event) {
        log.info("Создание платежа из события Kafka orderId={} userId={}", event.orderId(), event.userId());
        paymentMetrics.incrementEventReceived();

        Optional<Payment> existing = paymentRepository.findByOrderId(event.orderId())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREATED)
                .findFirst();

        if (existing.isPresent()) {
            paymentMetrics.incrementDuplicateSkipped();
            log.warn("Дубликат платежа из события orderId={} — пропускаем", event.orderId());
            return paymentMapper.toPaymentResponse(existing.get());
        }

        Payment payment = buildPayment(event.orderId(), event.userId(), event.paymentAmount());

        Payment saved = paymentRepository.save(payment);
        recordOutcome(saved);

        paymentEventProducer.sendPaymentCreatedEvent(saved);
        paymentMetrics.incrementEventSent();

        log.info("Платёж из события создан id={} orderId={} статус={}",
                saved.getId(), saved.getOrderId(), saved.getStatus());
        return paymentMapper.toPaymentResponse(saved);
    }

    @Override
    @Observed(name = "paymentservice.payments.get-by-order", contextualName = "get-payments-by-order")
    public List<PaymentResponse> getPaymentsByOrderId(int page, int size, Long orderId) {
        log.debug("Запрос платежей orderId={} page={} size={}", orderId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return paymentMapper.toPaymentResponseList(
                paymentRepository.findByOrderId(orderId, pageable).getContent());
    }

    @Override
    @Observed(name = "paymentservice.payments.get-by-user", contextualName = "get-payments-by-user")
    public List<PaymentResponse> getPaymentsByUserId(int page, int size, Long userId) {
        log.debug("Запрос платежей userId={} page={} size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return paymentMapper.toPaymentResponseList(
                paymentRepository.findAllByUserId(userId, pageable).getContent());
    }

    @Override
    @Observed(name = "paymentservice.payments.get-by-status", contextualName = "get-payments-by-status")
    public List<PaymentResponse> getPaymentsByStatus(int page, int size, PaymentStatus status) {
        log.debug("Запрос платежей статус={} page={} size={}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return paymentMapper.toPaymentResponseList(
                paymentRepository.findAllByStatus(status, pageable).getContent());
    }

    @Override
    @Observed(name = "paymentservice.payments.sum-by-period", contextualName = "get-all-payments-by-period")
    public BigDecimal getAllPaymentsByPeriod(int page, int size, Instant from, Instant to) {
        log.debug("Сумма платежей за период from={} to={}", from, to);
        List<Payment> payments = paymentRepository.findByTimestampBetweenAndStatus(from, to, PaymentStatus.CREATED);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Observed(name = "paymentservice.payments.user-sum-by-period", contextualName = "get-user-payments-by-period")
    public BigDecimal getUserPaymentsByPeriod(int page, int size, Long userId, Instant from, Instant to) {
        log.debug("Сумма платежей пользователя userId={} from={} to={}", userId, from, to);
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetweenAndStatus(
                userId, from, to, PaymentStatus.CREATED);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment buildPayment(Long orderId, Long userId, BigDecimal amount) {
        int randomNumber = randomNumberClient.getRandomNumber();
        PaymentStatus status = (randomNumber % 2 == 0) ? PaymentStatus.CREATED : PaymentStatus.ERROR;

        log.debug("randomNumber={} → статус платежа={}", randomNumber, status);

        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .timestamp(Instant.now())
                .paymentAmount(amount)
                .build();
    }

    private void recordOutcome(Payment payment) {
        if (payment.getStatus() == PaymentStatus.CREATED) {
            paymentMetrics.incrementCreated();
            paymentMetrics.recordPaymentAmount(payment.getPaymentAmount());
        } else {
            paymentMetrics.incrementError();
        }
    }
}
