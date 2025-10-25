package userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CardRequest(
        @NotNull(message = "User ID cannot be null")
        Long userId,

        @NotBlank(message = "Card number cannot be blank")
        @Size(max = 20)
        String number,

        @NotBlank(message = "Card holder cannot be blank")
        @Size(max = 200)
        String holder,

        @NotNull(message = "Expiration date cannot be null")
        LocalDate expirationDate
) {}
