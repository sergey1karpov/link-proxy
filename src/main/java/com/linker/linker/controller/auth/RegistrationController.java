package com.linker.linker.controller.auth;

import com.linker.linker.dto.auth.AuthResponseDto;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.entity.User;
import com.linker.linker.mapper.AuthMapper;
import com.linker.linker.service.auth.AuthService;
import com.linker.linker.service.auth.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Registration controller", description = "Регистрация нового пользователя")
public class RegistrationController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Value("${security.jwt.access}")
    private Duration accessTokenExpirationTime;

    @Value("${security.jwt.refresh}")
    private Duration refreshTokenExpirationTime;

    @PostMapping("/registration")
    @Operation(summary = "Регистрация нового пользователя")
    public ResponseEntity<Object> register(
            @Validated @RequestBody RegisterRequestDto request,
            BindingResult bindingResult
    ) {
        Map<String, String> errorsMessage = this.authService.registerValidate(
                request.getEmail(), request.getUsername(), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        User mappedUser = this.authMapper.toEntity(request);
        User user = this.authService.registerUser(mappedUser);

        String accessToken = this.jwtService.generateToken(user, this.accessTokenExpirationTime.toMillis());
        String refreshToken = this.jwtService.generateToken(user, this.refreshTokenExpirationTime.toMillis());

        return ResponseEntity.ok(new AuthResponseDto(accessToken, refreshToken));
    }
}
