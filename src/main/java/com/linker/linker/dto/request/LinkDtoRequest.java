package com.linker.linker.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linker.linker.entity.utils.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LinkDtoRequest {
    @NotNull(message = "Old url is required")
    @URL(message = "Must be a valid URL (e.g., https://example.com)")
    private String oldUrl;

    @Nullable
    @Enumerated(EnumType.STRING)
    private Status status;

    @Nullable
    private String privateCode;

    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    private LocalDateTime timeToLeave;
}
