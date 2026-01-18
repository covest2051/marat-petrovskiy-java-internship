package authenticationservice.dto;

public record TokenResponse(String accessToken, String refreshToken, Long expiresIn, Long userId) {}
