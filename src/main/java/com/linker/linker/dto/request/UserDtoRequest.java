package com.linker.linker.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@AllArgsConstructor
public class UserDtoRequest {
    @Nullable
    @Email(message = "Email is not valid")
    private String email;
}
