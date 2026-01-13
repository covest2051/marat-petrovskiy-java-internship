package orderservice.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataController {
    private final WebClient userWebClient;


    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserResponse getUserByEmail(String email) {
        return userWebClient.get()
                .uri("/users/email/{email}", email)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> {
                            if (resp.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                return Mono.empty();
                            }
                            return resp.createException();
                        })
                .bodyToMono(UserResponse.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(Mono::error)
                .block();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserResponse getUserById(Long id) {
        return userWebClient.get()
                .uri("/users/id/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> {
                            if (resp.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
                            }
                            return resp.createException();
                        })
                .bodyToMono(UserResponse.class)
                .timeout(Duration.ofSeconds(2))
                .block();
    }

    public String getUserByEmailFallback(String email, Throwable throwable) {
        return "User Service unavailable, returning fallback: " + throwable.getMessage();
    }

    public String getUserByIdFallback(Long id, Throwable throwable) {
        return "User Service unavailable, returning fallback: " + throwable.getMessage();
    }
}


