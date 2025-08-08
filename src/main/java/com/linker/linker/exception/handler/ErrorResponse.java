package com.linker.linker.exception.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponse {
    private final int status;
    private final String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    // Вариант без статуса (например, когда он задан по умолчанию)
    public ErrorResponse(String message) {
        this.status = HttpStatus.BAD_REQUEST.value();
        this.message = message;
    }
}