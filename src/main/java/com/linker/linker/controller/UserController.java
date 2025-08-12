package com.linker.linker.controller;

import com.linker.linker.dto.request.UserDtoRequest;
import com.linker.linker.entity.User;
import com.linker.linker.mapper.UserMapper;
import com.linker.linker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/user")
@Validated
@RequiredArgsConstructor
@Tag(name = "User controller", description = "Работа с пользователем")
public class UserController {
    private final UserMapper userMapper;
    private final UserService userService;

    @PatchMapping
    @Operation(summary = "Изменение пользователя")
    public void updateUser(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("user") UserDtoRequest userDtoRequest
    ) throws IOException {
        User mappedUser = userMapper.toEntity(userDtoRequest);

        this.userService.updateUserProfile(mappedUser, file);
    }
}
