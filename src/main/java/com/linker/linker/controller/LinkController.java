package com.linker.linker.controller;

import com.linker.linker.dto.request.LinkDtoRequest;
import com.linker.linker.entity.Link;
import com.linker.linker.handler.interfaces.OnCreate;
import com.linker.linker.handler.interfaces.OnUpdate;
import com.linker.linker.mapper.LinkMapper;
import com.linker.linker.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/link")
@RequiredArgsConstructor
@Validated
@Tag(name = "Link controller", description = "Работа с ссылками")
public class LinkController {
    private final LinkMapper linkMapper;
    private final LinkService linkService;

    /**
     * Создание ссылки
     * Ошибки валидации обрабатываем через GlobalExceptionHandler, а не через BindingResult
     * @param request - объект с данными
     * @return - сгенерированная ссылка
     */
    @PostMapping
    @Operation(summary = "Создание ссылки")
    public ResponseEntity<String> createLink(
            @Validated(OnCreate.class) @RequestBody LinkDtoRequest request
    ) {
        Link mapperLink = this.linkMapper.toEntity(request);

        return ResponseEntity.ok(this.linkService.createNewLink(mapperLink));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновление ссылки")
    public ResponseEntity<Link> updateOldLink(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody(required = false) LinkDtoRequest request
    ) {
        Link link = this.linkService.update(id, request);

        return ResponseEntity.ok(link);
    }
}
