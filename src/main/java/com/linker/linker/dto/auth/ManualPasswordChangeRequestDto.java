package com.linker.linker.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ManualPasswordChangeRequestDto {
    @NotNull(message = "Email can not be null")
    @Email(message = "You entered incorrect email")
    private String email;

    @Null
    private String error;
}
