package com.linker.linker.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
public class LoginRequestDto {
    @NotNull(message = "Message can not be null")
    @Length(message = "Your username must be in between 5 and 255 symbols", min = 5, max = 255)
    private String username;

    @NotNull(message = "Password can not be null")
    @Length(message = "Your password must be in between 5 and 255 symbols", min = 5, max = 255)
    private String password;
}
