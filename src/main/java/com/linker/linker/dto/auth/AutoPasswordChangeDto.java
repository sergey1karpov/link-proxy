package com.linker.linker.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AutoPasswordChangeDto {
    @NotNull(message = "secretCode not be null")
    private int secretCode;

    @Null
    private String error;
}
