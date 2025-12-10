package apigateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GatewayMetrics {

    private final Counter missingAuthHeader;

    @Autowired
    public GatewayMetrics(MeterRegistry registry) {
        this.missingAuthHeader = Counter.builder("gateway.auth.header.missing")
                .description("Missing Authorization header")
                .register(registry);
    }

    public void countMissingHeader() {
        missingAuthHeader.increment();
    }
}
