package userservice.dto;

import java.time.LocalDate;

public record UserInternalDto(Long id, String email, String name, String surname, LocalDate birthDate) {}
