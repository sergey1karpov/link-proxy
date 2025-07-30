package com.linker.linker.controller;

import com.linker.linker.dto.auth.LoginRequestDto;
import com.linker.linker.dto.auth.AuthResponseDto;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.entity.User;
import com.linker.linker.mapper.AuthMapper;
import com.linker.linker.service.auth.AuthService;
import com.linker.linker.service.auth.JwtService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthenticationManager authManager;
    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Value("${security.jwt.access}")
    private Duration accessTokenExpirationTime;

    @Value("${security.jwt.refresh}")
    private Duration refreshTokenExpirationTime;

    @PostMapping("/registration")
    public ResponseEntity<Object> register(
            @Validated @RequestBody RegisterRequestDto request,
            BindingResult bindingResult
    ) {
        List<String> errorsMessage = this.authService.registerValidate(
                request.getEmail(), request.getUsername(), bindingResult
        );
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        User mappedUser = this.authMapper.toEntity(request);
        User user = this.authService.registerUser(mappedUser);
        String jwt = this.jwtService.generateToken(user, this.accessTokenExpirationTime.toMillis());
        String refresh = this.jwtService.generateToken(user, this.refreshTokenExpirationTime.toMillis());

        return ResponseEntity.ok(new AuthResponseDto(jwt, refresh));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @Validated @RequestBody LoginRequestDto request,
            BindingResult bindingResult
    ) {
        List<String> errorsMessage = this.authService.loginValidate(request.getUsername(), bindingResult);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        Authentication auth = this.authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String jwt = this.jwtService.generateToken(user, this.accessTokenExpirationTime.toMillis());
        String refresh = this.jwtService.generateToken(user, this.refreshTokenExpirationTime.toMillis());
        return ResponseEntity.ok(new AuthResponseDto(jwt, refresh));
    }

    @PostMapping("/refresh-token")
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
