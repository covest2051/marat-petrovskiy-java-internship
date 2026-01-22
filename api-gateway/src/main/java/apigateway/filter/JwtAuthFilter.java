package apigateway.filter;

import apigateway.metrics.GatewayMetrics;
import apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final GatewayMetrics gatewayMetrics;

    private final List<String> excluded = List.of(
            "/auth/login",
            "/auth/register",
            "/actuator/**",
            "/register",
            "/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("Gateway Outgoing Headers: " + exchange.getRequest().getHeaders());
        String path = exchange.getRequest().getURI().getPath();

        System.out.println("DEBUG: Checking path: " + path);

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

        String authHeader = authHeaders.get(0);
        if (authHeader.length() <= 7) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is empty"));
        }
        String token = authHeader.substring(7);

        if (token.chars().filter(ch -> ch == '.').count() != 2) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed JWT structure"));
        }

        return jwtUtil.validateTokenReactive(token)
                .flatMap(claims -> {
                    Object userId = claims.get("userId");

                    System.out.println("DEBUG: Extracted userId from token: " + userId);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Role", String.valueOf(claims.get("role")))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(e -> {
                    System.err.println("DEBUG: JWT Validation Failed!");
                    e.printStackTrace();
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: " + e.getMessage()));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}