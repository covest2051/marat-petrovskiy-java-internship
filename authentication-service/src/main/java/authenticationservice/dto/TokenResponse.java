package authenticationservice.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {}
