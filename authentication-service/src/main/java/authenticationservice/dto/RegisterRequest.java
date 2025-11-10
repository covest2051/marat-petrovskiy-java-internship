package authenticationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    private String login;
    private String password;
    private String role;

    public RegisterRequest() {}

}
