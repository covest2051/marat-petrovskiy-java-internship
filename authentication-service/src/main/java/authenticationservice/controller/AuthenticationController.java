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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authenticationService.login(req.login(), req.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authenticationService.refresh(req.refreshToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authenticationService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        boolean ok = authenticationService.validateAccessToken(authHeader.substring(7));
        return ResponseEntity.ok(Map.of("valid", ok));
    }

    @PostMapping("/oauth/complete")
    public ResponseEntity<Void> completeOAuth(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        authenticationService.completeOAuthRegistration(authHeader.substring(7));
        return ResponseEntity.ok().build();
    }
}