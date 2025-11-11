package authenticationservice.controller;

import authenticationservice.dto.LoginRequest;
import authenticationservice.dto.RefreshRequest;
import authenticationservice.dto.RegisterRequest;
import authenticationservice.dto.TokenResponse;
import authenticationservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        TokenResponse tokens = authenticationService.login(req.login(), req.password());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authenticationService.refresh(req.refreshToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String role = Optional.ofNullable(req.getRole()).orElse("ROLE_USER");
        authenticationService.register(req.getLogin(), req.getPassword(), role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return ResponseEntity.status(400).build();
        String token = authHeader.substring(7);
        boolean ok = authenticationService.validateAccessToken(token);
        return ResponseEntity.ok(Map.of("valid", ok));
    }
}
