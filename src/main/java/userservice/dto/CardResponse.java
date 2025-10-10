package userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CardResponse(
        Long id,

        Long userId,

        @NotBlank(message = "Card number cannot be blank")
        @Size(max = 20)
        String number,

        @NotBlank(message = "Holder cannot be blank")
        @Size(max = 200)
        String holder,

        LocalDate expirationDate) {
}
