package com.linker.linker.controller.auth;

import com.linker.linker.dto.auth.ManualPasswordChangeRequestDto;
import com.linker.linker.dto.auth.ManualPasswordUpdateRequestDto;
import com.linker.linker.dto.mail.PasswordChangeDto;
import com.linker.linker.entity.utils.ResetType;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Manual reset password", description = "Ручное изменение пароля")
public class ManualResetPasswordController {
    private final AuthService authService;
    private final SendPasswordChangeMail sendManualPasswordChangeMail;

    @PostMapping("/manual-password-change")
    @Operation(summary = "Создание запроса на ручное изменение пароля")
    public ResponseEntity<Object> manualPasswordChangeRequest(
            @Validated @RequestBody ManualPasswordChangeRequestDto request,
            BindingResult bindingResult
    ) {
        this.authService.checkCountOfAttempts(request.getEmail(), bindingResult, ResetType.MANUAL);
        Map<String, String> errorsMessage = this.authService.emailValidate(
                request.getEmail(), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        String hash = this.authService.insertIntoResetTable(request.getEmail());

        this.sendManualPasswordChangeMail.sendToQueue(new PasswordChangeDto(
                request.getEmail(),
                "Manual reset password link",
                "http://localhost:8080/api/v1/auth/manual-password-change/" + hash
        ));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/manual-password-change/{hash}")
    @Operation(summary = "Ручное изменение пароля")
    public ResponseEntity<Object> manualPasswordChange(
            @PathVariable("hash") String hash,
            @Validated @RequestBody ManualPasswordUpdateRequestDto request,
            BindingResult bindingResult
    ) {
        this.authService.hashValidate(hash, bindingResult, ResetType.MANUAL);
        Map<String, String> errorsMessage = this.authService.compareOldUserPassword(
                hash, request.getOldPassword(), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        this.authService.replaceOldPassword(hash, request.getNewPassword());

        return ResponseEntity.ok().build();
    }
}
