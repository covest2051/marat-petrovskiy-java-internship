package paymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;

@Component
public class PaymentMetrics {

    private final Counter createdCounter;
    private final Counter errorCounter;
    private final Counter duplicateSkippedCounter;

    private final Counter eventReceivedCounter;
    private final Counter eventSentCounter;

    private final DistributionSummary paymentAmountSummary;

    public PaymentMetrics(MeterRegistry registry) {
        this.createdCounter = Counter.builder("paymentservice.payments.created")
                .description("Успешно созданные платежи (статус CREATED)")
                .register(registry);

        this.errorCounter = Counter.builder("paymentservice.payments.error")
                .description("Платежи со статусом ERROR после вызова randomNumberClient")
                .register(registry);

        this.duplicateSkippedCounter = Counter.builder("paymentservice.payments.duplicate_skipped")
                .description("Пропущенные дубликаты — платёж для orderId уже существовал")
                .register(registry);

        this.eventReceivedCounter = Counter.builder("paymentservice.kafka.order_events_received")
                .description("Получено OrderCreatedEvent из Kafka")
                .register(registry);

        this.eventSentCounter = Counter.builder("paymentservice.kafka.payment_events_sent")
                .description("Отправлено PaymentCreatedEvent в Kafka")
                .register(registry);

        this.paymentAmountSummary = DistributionSummary.builder("paymentservice.payments.amount")
                .description("Распределение сумм платежей")
                .baseUnit("currency_units")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incrementCreated() {
        createdCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

    public void incrementDuplicateSkipped() {
        duplicateSkippedCounter.increment();
    }

    public void incrementEventReceived() {
        eventReceivedCounter.increment();
    }

    public void incrementEventSent() {
        eventSentCounter.increment();
    }

    public void recordPaymentAmount(BigDecimal amount) {
        if (amount != null) {
            paymentAmountSummary.record(amount.doubleValue());
        }
    }
}