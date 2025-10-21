package userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Surname cannot be blank")
        @Size(max = 100)
        String surname,

        LocalDate birthDate,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must match pattern: email@example.com")
        String email
) {}
