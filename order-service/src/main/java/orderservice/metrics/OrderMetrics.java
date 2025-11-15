package orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter ordersCreated;
    private final Counter ordersCancelled;

    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders_created_total")
                .description("Total number of orders created")
                .register(registry);
        this.ordersCancelled = Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .register(registry);
    }

    public void incrementCreated() {
        ordersCreated.increment();
    }

    public void incrementCancelled() {
        ordersCancelled.increment();
    }
}
