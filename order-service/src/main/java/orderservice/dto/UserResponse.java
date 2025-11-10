package orderservice.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,

        String name,

        String surname,

        LocalDate birthDate,

        String email) {
}
