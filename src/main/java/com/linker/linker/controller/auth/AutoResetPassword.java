package com.linker.linker.controller.auth;

import com.linker.linker.dto.auth.AutoPasswordChangeDto;
import com.linker.linker.dto.auth.ManualPasswordChangeRequestDto;
import com.linker.linker.dto.mail.PasswordChangeDto;
import com.linker.linker.entity.User;
import com.linker.linker.entity.utils.ResetType;
import com.linker.linker.exception.UserNotFoundException;
import com.linker.linker.mail.SendPasswordChangeMail;
import com.linker.linker.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auto reset password", description = "Автоматическое изменение пароля")
public class AutoResetPassword {
    private final AuthService authService;
    private final SendPasswordChangeMail sendManualPasswordChangeMail;

    @PostMapping("/auto-reset-password")
    @Operation(summary = "Запрос на автоматическое изменение пароля")
    public ResponseEntity<Object> createAutoResetPasswordLink(
            @Validated @RequestBody ManualPasswordChangeRequestDto request,
            BindingResult bindingResult
    ) {
        this.authService.checkCountOfAttempts(request.getEmail(), bindingResult, ResetType.AUTO);
        Map<String, String> errorsMessage = this.authService.emailValidate(
                request.getEmail(), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        Map<String, String> data = this.authService.saveSecretCode(request.getEmail());

        this.sendManualPasswordChangeMail.sendToQueue(new PasswordChangeDto(
                request.getEmail(),
                "Auto reset password link",
                "http://localhost:8080/api/v1/auth/auto-password-change/" + data.get("hash") + ", code: " + data.get("secretCode")
        ));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/auto-password-change/{hash}")
    @Operation(summary = "Автоматический сброс пароля")
    public ResponseEntity<Object> autoPasswordChange(
            @PathVariable("hash") String hash,
            @Validated @RequestBody AutoPasswordChangeDto request, // Данные из тела запроса
            BindingResult bindingResult
    ) {
        this.authService.hashValidate(hash, bindingResult, ResetType.AUTO);
        Map<String, String> errorsMessage = this.authService.compareSecretCode(
                hash, String.valueOf(request.getSecretCode()), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        Optional<User> user = this.authService.getChangedUser(hash);
        if (user.isEmpty()) throw new UserNotFoundException("User not found");

        String newPassword = this.authService.autoChangePassword(user.get());

        this.sendManualPasswordChangeMail.sendToQueue(new PasswordChangeDto(
                user.get().getEmail(),
                "New password",
                "New password: " + newPassword
        ));

        return ResponseEntity.ok().build();
    }
}
