package authenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record RegisterRequest(
        @JsonProperty("login") String login,
        @JsonProperty("password") String password,
        @JsonProperty("role") String role,
        @JsonProperty("name") String name,
        @JsonProperty("surname") String surname,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("birthDate") LocalDate birthDate
) {
}
