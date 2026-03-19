package apigateway.filter;

import apigateway.metrics.GatewayMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final GatewayMetrics gatewayMetrics;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String userId = auth.getToken().getClaimAsString("userId");
                    String role   = auth.getToken().getClaimAsString("role");

                    log.debug("Пропускаем запрос userId={} path={}",
                            userId, exchange.getRequest().getURI().getPath());

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",   userId != null ? userId : "")
                            .header("X-User-Role", role   != null ? role   : "")
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    String path = exchange.getRequest().getURI().getPath();
                    if (path.startsWith("/auth/") || path.startsWith("/actuator/")) {
                        return chain.filter(exchange);
                    }
                    gatewayMetrics.countMissingHeader();
                    log.warn("Запрос без аутентификации достиг JwtAuthFilter path={}", path);
                    return chain.filter(exchange);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}