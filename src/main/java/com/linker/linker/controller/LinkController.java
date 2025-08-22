package com.linker.linker.controller;

import com.linker.linker.dto.request.LinkDtoRequest;
import com.linker.linker.entity.Link;
import com.linker.linker.handler.interfaces.OnCreate;
import com.linker.linker.handler.interfaces.OnUpdate;
import com.linker.linker.mapper.LinkMapper;
import com.linker.linker.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Map<String, String>> createLink(
            @Validated(OnCreate.class) @RequestBody LinkDtoRequest request
    ) {
        String shortLink = this.linkService.createNewLink(this.linkMapper.toEntity(request));

        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=http://localhost/cc/" + shortLink;

        return ResponseEntity.ok(Map.of(
                "link", shortLink,
                "qrCode", qrCodeUrl
        ));
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Обновление ссылки")
    public ResponseEntity<Link> updateOldLink(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody(required = false) LinkDtoRequest request
    ) {
        Link link = this.linkService.update(id, request);

        return ResponseEntity.ok(link);
    }

    @GetMapping("/all")
    @Operation(summary = "Получение всех ссылок")
    public ResponseEntity<Page<Link>> getAllLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(this.linkService.getAll(pageable));
    }
}
