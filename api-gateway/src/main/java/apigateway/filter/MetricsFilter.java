package apigateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry registry;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        Counter.builder("gateway.route.calls")
                .description("Number of calls per route")
                .tag("route", path)
                .register(registry)
                .increment();

        Counter.builder("gateway.rps")
                .description("Requests per second")
                .register(registry)
                .increment();

        long start = System.currentTimeMillis();

        return chain.filter(exchange)
                .doOnSuccess((_) -> {
                    long duration = System.currentTimeMillis() - start;

                    Timer.builder("gateway.route.latency")
                            .description("Request latency per route")
                            .tag("route", path)
                            .register(registry)
                            .record(duration, TimeUnit.MILLISECONDS);

                    String status = String.valueOf(exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().value() : 0);
                    Counter.builder("gateway.route.status")
                            .description("HTTP status codes per route")
                            .tag("route", path)
                            .tag("status", status)
                            .register(registry)
                            .increment();
                });
    }
}
