package authenticationservice.service;

import authenticationservice.dto.KeycloakTokenResponse;
import authenticationservice.dto.RegisterRequest;
import authenticationservice.dto.TokenResponse;
import authenticationservice.dto.UserRegistrationDto;
import authenticationservice.entity.KeycloakUserMapping;
import authenticationservice.repository.KeycloakUserMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String REALM = "internship";

    private final KeycloakUserMappingRepository mappingRepository;
    private final Keycloak keycloakAdmin;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public TokenResponse login(String login, String password) {
        log.debug("Попытка входа login={}", login);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", clientId);
        params.add("username", login);
        params.add("password", password);

        try {
            KeycloakTokenResponse keycloakResponse = callTokenEndpoint(params);
            Long userId = extractClaim(keycloakResponse.accessToken(), "userId").asLong();

            log.info("Вход выполнен успешно login={} userId={}", login, userId);
            return new TokenResponse(
                    keycloakResponse.accessToken(),
                    keycloakResponse.refreshToken(),
                    keycloakResponse.expiresIn(),
                    userId
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Неверные учётные данные login={}", login);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Регистрация нового пользователя login={}", request.login());

        if (mappingRepository.existsByLogin(request.login())) {
            log.warn("Регистрация отклонена — логин уже занят login={}", request.login());
            throw new IllegalArgumentException("Login exists");
        }

        String role = resolveRole(request.role());

        UsersResource usersResource = keycloakAdmin.realm(REALM).users();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.login());
        user.setEmail(request.login());
        user.setEnabled(true);

        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            log.error("Keycloak вернул статус {} при создании пользователя login={}", response.getStatus(), request.login());
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
        }

        String locationPath = response.getLocation().getPath();
        String keycloakId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
        log.debug("Пользователь создан в Keycloak keycloakId={}", keycloakId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);
        usersResource.get(keycloakId).resetPassword(credential);

        RoleRepresentation roleRep = keycloakAdmin.realm(REALM).roles().get(role).toRepresentation();
        usersResource.get(keycloakId).roles().realmLevel().add(List.of(roleRep));

        KeycloakUserMapping mapping = KeycloakUserMapping.builder()
                .login(request.login())
                .role(role)
                .keycloakId(keycloakId)
                .createdAt(Instant.now())
                .build();
        mapping = mappingRepository.save(mapping);

        UserRepresentation userUpdate = new UserRepresentation();
        userUpdate.setAttributes(Map.of(
                "userId", List.of(String.valueOf(mapping.getId())),
                "role",   List.of(role)
        ));
        usersResource.get(keycloakId).update(userUpdate);

        UserRegistrationDto profileData = new UserRegistrationDto(
                mapping.getId(),
                request.login(),
                request.name(),
                request.surname(),
                request.birthDate(),
                role
        );
        try {
            restTemplate.postForEntity(userServiceUrl + "/internal/users", profileData, Void.class);
        } catch (Exception e) {
            log.error("Не удалось создать профиль в user-service для login={}: {}", request.login(), e.getMessage());

            usersResource.delete(keycloakId);
            throw new RuntimeException("Не удалось создать профиль в User Service: " + e.getMessage());
        }

        log.info("Пользователь зарегистрирован login={} userId={} role={}", request.login(), mapping.getId(), role);
    }

    public TokenResponse refresh(String refreshToken) {
        log.debug("Запрос обновления токена");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("refresh_token", refreshToken);

        try {
            KeycloakTokenResponse keycloakResponse = callTokenEndpoint(params);
            Long userId = extractClaim(keycloakResponse.accessToken(), "userId").asLong();

            log.info("Токен обновлён userId={}", userId);
            return new TokenResponse(
                    keycloakResponse.accessToken(),
                    keycloakResponse.refreshToken(),
                    keycloakResponse.expiresIn(),
                    userId
            );
        } catch (HttpClientErrorException.BadRequest e) {
            log.warn("Невалидный refresh-токен: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    @Transactional
    public void completeOAuthRegistration(String accessToken) {
        String keycloakId = extractClaim(accessToken, "sub").asText();
        String email      = extractClaim(accessToken, "email").asText();

        log.info("Завершение OAuth-регистрации keycloakId={} email={}", keycloakId, email);

        if (mappingRepository.findByKeycloakId(keycloakId).isPresent()) {
            log.debug("Маппинг уже существует для keycloakId={}", keycloakId);
            return;
        }

        KeycloakUserMapping mapping = KeycloakUserMapping.builder()
                .login(email)
                .role("ROLE_USER")
                .keycloakId(keycloakId)
                .createdAt(Instant.now())
                .build();
        mapping = mappingRepository.save(mapping);

        UserRepresentation userUpdate = new UserRepresentation();
        userUpdate.setAttributes(Map.of(
                "userId", List.of(String.valueOf(mapping.getId())),
                "role",   List.of("ROLE_USER")
        ));
        keycloakAdmin.realm(REALM).users().get(keycloakId).update(userUpdate);

        UserRegistrationDto profileData = new UserRegistrationDto(
                mapping.getId(), email, email, "", null, "ROLE_USER"
        );
        try {
            restTemplate.postForEntity(userServiceUrl + "/internal/users", profileData, Void.class);
        } catch (Exception e) {
            log.error("Не удалось создать профиль в user-service для OAuth пользователя keycloakId={}: {}", keycloakId, e.getMessage());
            throw new RuntimeException("Не удалось создать профиль: " + e.getMessage());
        }

        log.info("OAuth-регистрация завершена userId={} email={}", mapping.getId(), email);
    }

    public boolean validateAccessToken(String token) {
        try {
            extractClaim(token, "sub");
            return true;
        } catch (Exception e) {
            log.debug("Невалидный токен: {}", e.getMessage());
            return false;
        }
    }

    private KeycloakTokenResponse callTokenEndpoint(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return restTemplate.postForObject(
                keycloakUrl + "/realms/" + REALM + "/protocol/openid-connect/token",
                new HttpEntity<>(params, headers),
                KeycloakTokenResponse.class
        );
    }

    private JsonNode extractClaim(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(decoded).get(claim);
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось прочитать claim '" + claim + "' из токена", e);
        }
    }

    private String resolveRole(String role) {
        if (role == null || role.isBlank()) return "ROLE_USER";
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
