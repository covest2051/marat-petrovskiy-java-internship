package apigateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

record RegisterRequest(String email, String password, String name, String surname) {
}

record AuthResponse(Long userId, String email) {
}

record UserResponse(Long id, String email, String name, String surname) {
}

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GatewayController {

    private final WebClient authWebClient;
    private final WebClient userWebClient;

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody RegisterRequest request) {
        return authWebClient.post()
                .uri("/auth/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .flatMap(authResp ->
                        userWebClient.post()
                                .uri("/users")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(UserResponse.class)
                                .onErrorResume(e -> rollbackAuth(authResp.userId())
                                        .then(Mono.error(new RuntimeException("Registration failed, rollback executed"))))
                )
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body("User registered successfully with ID: " + user.id()));
    }

    private Mono<Void> rollbackAuth(Long userId) {
        return authWebClient.delete()
                .uri("/auth/{id}", userId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> System.out.println("Rolled back Authentication Service for userId=" + userId));
    }
}

