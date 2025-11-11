package orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import orderservice.dto.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserClient {

    private final WebClient userWebClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserResponse getUserById(Long id) {
        return userWebClient.get()
                .uri("/users/id/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> Mono.error(new RuntimeException("User Service returned error: " + resp.statusCode()))
                )
                .bodyToMono(UserResponse.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    public UserResponse getUserByIdFallback(Long id, Throwable ex) {
        System.err.println("Fallback triggered for getUserById: " + id + ", due to: " + ex.getMessage());
        return new UserResponse(id, "Unknown User", null, null, null);
    }
}

