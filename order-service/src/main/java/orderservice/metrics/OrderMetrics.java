package orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter createdCounter;
    private final Counter deletedCounter;
    private final Counter notFoundCounter;
    private final Counter illegalStatusChangeCounter;
    public OrderMetrics(MeterRegistry registry) {
        this.createdCounter = Counter.builder("orderservice.orders.created")
                .description("Количество успешно созданных заказов")
                .register(registry);

        this.deletedCounter = Counter.builder("orderservice.orders.deleted")
                .description("Количество удалённых заказов")
                .register(registry);

        this.notFoundCounter = Counter.builder("orderservice.orders.not_found")
                .description("Количество обращений к несуществующему заказу")
                .register(registry);

        this.illegalStatusChangeCounter = Counter.builder("orderservice.orders.illegal_status_change")
                .description("Попытки перевести заказ в недопустимый статус")
                .register(registry);
    }

    public void incrementCreated() {
        createdCounter.increment();
    }

    public void incrementDeleted() {
        deletedCounter.increment();
    }

    public void incrementNotFound() {
        notFoundCounter.increment();
    }

    public void incrementIllegalStatusChange() {
        illegalStatusChangeCounter.increment();
    }
}
