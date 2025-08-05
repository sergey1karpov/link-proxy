package com.linker.linker.controller;

import com.linker.linker.dto.auth.*;
import com.linker.linker.dto.mail.PasswordChangeDto;
import com.linker.linker.entity.User;
import com.linker.linker.mail.SendPasswordChangeMail;
import com.linker.linker.mapper.AuthMapper;
import com.linker.linker.service.auth.AuthService;
import com.linker.linker.service.auth.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth controller", description = "Auth API")
public class AuthController {
    private final AuthenticationManager authManager;
    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final SendPasswordChangeMail sendManualPasswordChangeMail;

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

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<Object> login(
            @Validated @RequestBody LoginRequestDto request, BindingResult bindingResult
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
    @Operation(summary = "Получение рефреш токена")
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

    @PostMapping("/manual-password-change")
    @Operation(summary = "Заявка на ручную смену пароля")
    public ResponseEntity<Object> manualPasswordChangeRequest(
            @Validated @RequestBody ManualPasswordChangeRequestDto request, BindingResult bindingResult
    ) {
        //Валидация на кол-во попыток отправления по одному email
        this.authService.checkThrottle(request.getEmail(), bindingResult);
        //Валидация на то, что в бд есть юзер с указанным email
        List<String> errorsMessage = this.authService.emailValidate(request.getEmail(), bindingResult);
        //Валидация введенных данных и отображение всех валидационных ошибок
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }

        //делаем запись в бд и получаем hash
        String hash = this.authService.insertIntoResetTable(request.getEmail());

        //Формируем письмо с кешем и отправляем юзеру ссылку с хешем
        //TODO: переделать отправку письма
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
        //проверить валидность хеша по времени, если хеш не валиден, то показать валидационную ошибку
        //хеш валиден 10 минут
        this.authService.hashValidate(hash, bindingResult);

        //проверить на валидность старый пароль что он совпадает
        this.authService.compareOldUserPassword(hash, request.getOldPassword(), bindingResult);

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());

            return ResponseEntity.badRequest().body(errorMessages);
        }

        System.out.println("OK");
        //если все условия соблюдены, то меняем старый пароль на новый
        this.authService.replaceOldPassword(hash, request.getNewPassword());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/auto-reset-password")
    @Operation(summary = "Запрос на автоматическое изменение пароля")
    public ResponseEntity<Object> createAutoResetPasswordLink(
            @Validated @RequestBody ManualPasswordChangeRequestDto request, BindingResult bindingResult
    ) {
        this.authService.checkThrottle(request.getEmail(), bindingResult);
        List<String> errorsMessage = this.authService.emailValidate(request.getEmail(), bindingResult);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(errorsMessage);
        }
        System.out.println("OK");

        //формируем код
        int code = this.authService.generateResetPasswordSecretCode();

        //формируем хеш
        String hash = UUID.randomUUID().toString();

        //дастаем юзера
        //TODO: чекнуть на то, что юзер есть
        Optional<User> user = this.authService.getUserByEmail(request.getEmail());

        this.authService.saveSecretCode(user.get().getId(), request.getEmail(), code, hash);
//
//        //TODO: переделать отправку письма
        this.sendManualPasswordChangeMail.sendToQueue(new PasswordChangeDto(
                request.getEmail(),
                "Auto reset password link",
                "http://localhost:8080/api/v1/auth/auto-password-change/" + hash + ", code: " + code
        ));
        //4. если есть, то делаем запись в бд(id, user_id(связь с таблицей юзер, OneToMany где у юзера может быть много ссылок и только одна ссылка к юзеру), secret_code, created_at(timestamp, null)) и отправляем юзеру ссылку с хешем
        //5. юзер переходит по ссылке и мы генерируем новый пароль и меняем его в бд
        //6. возвращаем новый пароль письмом
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auto-password-change/{hash}")
    @Operation(summary = "Автоматический сброс пароля")
    public ResponseEntity<Object> autoPasswordChange(
            @PathVariable("hash") String hash,
            @Validated @RequestBody AutoPasswordChangeDto request, // Данные из тела запроса
            BindingResult bindingResult
    ) {
        //проверить валидность хеша по времени, если хеш не валиден, то показать валидационную ошибку
        //хеш валиден 10 минут
        this.authService.hashValidate(hash, bindingResult);

        Optional<User> user = this.authService.getChangedUser(hash, String.valueOf(request.getSecretCode()), bindingResult);

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());

            return ResponseEntity.badRequest().body(errorMessages);
        }

        String newPassword = this.authService.autoChangePassword(user.get());

        this.sendManualPasswordChangeMail.sendToQueue(new PasswordChangeDto(
                user.get().getEmail(),
                "New password",
                "New password: " + newPassword
        ));

        return ResponseEntity.ok().build();
    }
}
