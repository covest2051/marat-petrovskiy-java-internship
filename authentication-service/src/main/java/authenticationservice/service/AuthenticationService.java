package authenticationservice.service;

import authenticationservice.dto.RegisterRequest;
import authenticationservice.dto.TokenResponse;
import authenticationservice.entity.UserCredential;
import authenticationservice.repository.UserCredentialRepository;
import authenticationservice.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RestTemplate restTemplate;

    private final String USER_SERVICE_URL = "http://user-service:8080/internal/users";

    public TokenResponse login(String login, String password) {
        UserCredential user = userCredentialRepository.findByLogin(login).orElseThrow(() -> new BadCredentialsException("Invalid login"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String access = jwtProvider.generateAccessToken(user.getId(), login, user.getRole());
        String refresh = jwtProvider.generateRefreshToken(login);
        user.setRefreshToken(refresh);
        userCredentialRepository.save(user);
        return new TokenResponse(access, refresh, jwtProvider.getAccessExpirationMs(), user.getId());
    }

    public TokenResponse refresh(String refreshToken) {
        try {
            String login = jwtProvider.getLoginFromToken(refreshToken);
            UserCredential user = userCredentialRepository.findByLogin(login).orElseThrow();
            if (!refreshToken.equals(user.getRefreshToken())) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            String newAccess = jwtProvider.generateAccessToken(user.getId(), login, user.getRole());
            String newRefresh = jwtProvider.generateRefreshToken(login);
            user.setRefreshToken(newRefresh);
            userCredentialRepository.save(user);
            return new TokenResponse(newAccess, newRefresh, jwtProvider.getAccessExpirationMs(), user.getId());
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userCredentialRepository.existsByLogin(request.getLogin()))
            throw new IllegalArgumentException("Login exists");

        UserCredential u = new UserCredential();
        String role = request.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        u.setLogin(request.getLogin());
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        u.setRole(role);
        u.setCreatedAt(Instant.now());

        u = userCredentialRepository.save(u);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", u.getId());
        profileData.put("email", request.getLogin());
        profileData.put("name", request.getName());
        profileData.put("surname", request.getSurname());

        try {
            restTemplate.postForEntity(USER_SERVICE_URL, profileData, Void.class);
        } catch (Exception e) {
            // Если user-service недоступен, транзакция откатится
            // и пользователь в auth-service не будет создан
            throw new RuntimeException("Не удалось создать профиль в User Service: " + e.getMessage());
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            jwtProvider.validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

