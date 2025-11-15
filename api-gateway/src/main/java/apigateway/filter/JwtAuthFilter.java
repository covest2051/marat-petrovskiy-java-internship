package apigateway.filter;

import apigateway.metrics.GatewayMetrics;
import apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final GatewayMetrics gatewayMetrics;

    private final List<String> excluded = List.of(
            "/auth/login",
            "/auth/register",
            "/actuator/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        for (String pattern : excluded) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        ServerHttpRequest request = exchange.getRequest();
        List<String> authHeaders = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")) {
            gatewayMetrics.countMissingHeader();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeaders.get(0).substring(7);

        return jwtUtil.validateTokenReactive(token)
                .flatMap(claims -> {
                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(claims.get("userId")))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")));
    }
}


