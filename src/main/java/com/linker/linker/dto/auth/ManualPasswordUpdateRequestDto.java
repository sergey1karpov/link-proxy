package com.linker.linker.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
public class ManualPasswordUpdateRequestDto {
    @NotNull(message = "Old password can not be null")
    @Length(min = 6, max = 255)
    private String oldPassword;

    @NotNull(message = "New password can not be null")
    @Length(min = 6, max = 255)
    private String newPassword;

    @Null
    private String error;
}
