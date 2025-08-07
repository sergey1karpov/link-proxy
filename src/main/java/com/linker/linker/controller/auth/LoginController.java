package com.linker.linker.controller.auth;

import com.linker.linker.dto.auth.AuthResponseDto;
import com.linker.linker.dto.auth.LoginRequestDto;
import com.linker.linker.service.auth.AuthService;
import com.linker.linker.service.auth.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
@Tag(name = "Login controller", description = "Логин и получение рефреш токена")
public class LoginController {
    private final AuthenticationManager authManager;
    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${security.jwt.access}")
    private Duration accessTokenExpirationTime;

    @Value("${security.jwt.refresh}")
    private Duration refreshTokenExpirationTime;

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<Object> login(
            @Validated @RequestBody LoginRequestDto request,
            BindingResult bindingResult
    ) {
        Map<String, String> errorsMessage = this.authService.loginValidate(
                request.getUsername(), request.getPassword(), bindingResult
        );

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        Authentication auth = this.authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();

        String accessToken = this.jwtService.generateToken(user, this.accessTokenExpirationTime.toMillis());
        String refreshToken = this.jwtService.generateToken(user, this.refreshTokenExpirationTime.toMillis());

        return ResponseEntity.ok(new AuthResponseDto(accessToken, refreshToken));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Получение refresh token")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        String username = this.jwtService.extractUsername(refreshToken);
        UserDetails user = this.authService.loadUserByUsername(username);

        if (!this.jwtService.isTokenValid(refreshToken, user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = this.jwtService.generateToken(user, this.accessTokenExpirationTime.toMillis());

        return ResponseEntity.ok(new AuthResponseDto(newAccessToken, refreshToken));
    }
}
