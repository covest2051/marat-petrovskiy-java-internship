package authenticationservice.service;

import authenticationservice.dto.RegisterRequest;
import authenticationservice.dto.TokenResponse;
import authenticationservice.dto.UserRegistrationDto;
import authenticationservice.entity.UserCredential;
import authenticationservice.metrics.AuthMetrics;
import authenticationservice.repository.UserCredentialRepository;
import authenticationservice.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RestTemplate restTemplate;
    private final AuthMetrics authMetrics;

    private final String USER_SERVICE_URL = "http://user-service:8080/internal/users";

    public TokenResponse login(String login, String password) {
        log.debug("Попытка входа login={}", login);

        return authMetrics.loginTimer().record(() -> {

            UserCredential user = userCredentialRepository.findByLogin(login)
                    .orElseThrow(() -> {
                        log.warn("Логин не найден login={}", login);
                        return new BadCredentialsException("Invalid credentials");
                    });

            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                authMetrics.incrementLoginBadCredentials();
                log.warn("Неверный пароль для login={}", login);
                throw new BadCredentialsException("Invalid credentials");
            }

            String access = jwtProvider.generateAccessToken(user.getId(), login, user.getRole());
            String refresh = jwtProvider.generateRefreshToken(login);
            user.setRefreshToken(refresh);
            userCredentialRepository.save(user);
            return new TokenResponse(access, refresh, jwtProvider.getAccessExpirationMs(), user.getId());
        });
    }

    public TokenResponse refresh(String refreshToken) {
        log.debug("Запрос обновления токена");

        try {
            String login = jwtProvider.getLoginFromToken(refreshToken);
            UserCredential user = userCredentialRepository.findByLogin(login).orElseThrow();
            if (!refreshToken.equals(user.getRefreshToken())) {
                log.warn("Невалидный refresh-токен для login={}", login);
                throw new BadCredentialsException("Invalid refresh token");
            }
            String newAccess = jwtProvider.generateAccessToken(user.getId(), login, user.getRole());
            String newRefresh = jwtProvider.generateRefreshToken(login);
            user.setRefreshToken(newRefresh);
            userCredentialRepository.save(user);

            log.info("Токен обновлён для login={}", login);

            return new TokenResponse(newAccess, newRefresh, jwtProvider.getAccessExpirationMs(), user.getId());
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Регистрация нового пользователя login={} role={}", request.login(), request.role());

        if (userCredentialRepository.existsByLogin(request.login()))
            throw new IllegalArgumentException("Login exists");

        UserCredential u = new UserCredential();
        String role = request.role();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        u.setLogin(request.login());
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setRole(role);
        u.setCreatedAt(Instant.now());

        u = userCredentialRepository.save(u);

        UserRegistrationDto profileData = new UserRegistrationDto(
                u.getId(),
                request.login(),
                request.name(),
                request.surname(),
                request.birthDate(),
                role
        );

        try {
            restTemplate.postForEntity(USER_SERVICE_URL, profileData, Void.class);
        } catch (Exception e) {
            log.error("Error creating profile: " + e.getMessage());
            throw new RuntimeException("Не удалось создать профиль в User Service: " + e.getMessage());
        }

        authMetrics.incrementRegisterSuccess();
        log.info("Пользователь успешно зарегистрирован login={} userId={} tole={}", request.login(), u.getId(), role);
    }

    public boolean validateAccessToken(String token) {
        try {
            jwtProvider.validateToken(token);
            return true;
        } catch (JwtException e) {
            log.debug("Невалидный access-токен: {}", e.getMessage());
            return false;
        }
    }
}

