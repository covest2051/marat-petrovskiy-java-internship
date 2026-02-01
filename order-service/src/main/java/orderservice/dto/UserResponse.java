package orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
        Long id,

        String name,

        String surname,

        LocalDate birthDate,

        String email) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
