package authenticationservice.dto;

import java.time.LocalDate;

public record UserRegistrationDto(Long id, String email, String name, String surname, LocalDate birthDate) {}
