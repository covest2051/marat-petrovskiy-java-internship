package authenticationservice.service;

import authenticationservice.dto.TokenResponse;
import authenticationservice.entity.UserCredential;
import authenticationservice.repository.UserCredentialRepository;
import authenticationservice.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse login(String login, String password) {
        UserCredential user = userCredentialRepository.findByLogin(login).orElseThrow(() -> new BadCredentialsException("Invalid login"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String access = jwtProvider.generateAccessToken(login, user.getRole());
        String refresh = jwtProvider.generateRefreshToken(login);
        user.setRefreshToken(refresh);
        userCredentialRepository.save(user);
        return new TokenResponse(access, refresh, jwtProvider.getAccessExpirationMs());
    }

    public TokenResponse refresh(String refreshToken) {
        try {
            String login = jwtProvider.getLoginFromToken(refreshToken);
            UserCredential user = userCredentialRepository.findByLogin(login).orElseThrow();
            if (!refreshToken.equals(user.getRefreshToken())) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            String newAccess = jwtProvider.generateAccessToken(login, user.getRole());
            String newRefresh = jwtProvider.generateRefreshToken(login);
            user.setRefreshToken(newRefresh);
            userCredentialRepository.save(user);
            return new TokenResponse(newAccess, newRefresh, jwtProvider.getAccessExpirationMs());
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    public void register(String login, String rawPassword, String role) {
        if (userCredentialRepository.existsByLogin(login)) throw new IllegalArgumentException("Login exists");
        UserCredential u = new UserCredential();
        u.setLogin(login);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setCreatedAt(Instant.now());
        userCredentialRepository.save(u);
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

