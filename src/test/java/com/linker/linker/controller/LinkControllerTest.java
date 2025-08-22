package com.linker.linker.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.dto.request.LinkDtoRequest;
import com.linker.linker.entity.Link;
import com.linker.linker.entity.User;
import com.linker.linker.entity.utils.Status;
import com.linker.linker.repository.LinkRepository;
import com.linker.linker.repository.UserRepository;
import com.linker.linker.service.auth.JwtService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // используем application-test.properties с H2
@Transactional
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;

    private Optional<User> user;

    @BeforeEach
    void setUp() throws Exception {
        // Создаем пользователя для получения токена
        RegisterRequestDto requestDto = new RegisterRequestDto(
                "TestUser",
                "email@email.com",
                "q1w2e3r4"
        );

        MvcResult result = this.mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseJson, new TypeReference<>() {});
        this.accessToken = responseMap.get("accessToken");

        //Получаем юзера
        String username = this.jwtService.extractUsername(this.accessToken);
        this.user = this.userRepository.findByUsername(username);

        // Очищаем базу перед каждым тестом
        linkRepository.deleteAll();
    }

    @Test
    void createLink() throws Exception {
        LinkDtoRequest request = new LinkDtoRequest(
                "https://example-test.com",
                Status.PUBLIC,
                "12345",
                LocalDateTime.now()
        );

        MvcResult result = mockMvc.perform(post("/api/v1/link")
                        .header("Authorization", "Bearer " + this.accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").isNotEmpty())
                .andExpect(jsonPath("$.qrCode").isNotEmpty())
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(result.getResponse().getContentAsString());
        String linkHash = rootNode.get("link").asText();
        Optional<Link> link = this.linkRepository.findByHash(linkHash);

        assertEquals(1, linkRepository.count());
        assertEquals(request.getOldUrl(), link.get().getOldUrl());
    }

    @Test
    void updateOldLink() throws Exception {
        //Создание ссылки
        Link link = new Link();
        link.setOldUrl("https://example-test.com");
        link.setNewUrl("qwerty");
        link.setStatus(Status.PUBLIC);
        this.linkRepository.save(link);

        //Новые входные данные
        LinkDtoRequest request = new LinkDtoRequest(
                "https://example-test2.com",
                Status.PRIVATE,
                "12345",
                LocalDateTime.of(2025, 12, 31, 23, 55)
        );

        //Создание запроса API на изменение ссылки
        mockMvc.perform(patch("/api/v1/link/" + link.getId())
                        .header("Authorization", "Bearer " + this.accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldUrl").value("https://example-test2.com"))
                .andExpect(jsonPath("$.status").value("PRIVATE"))
                .andExpect(jsonPath("$.privateCode").value("12345"));

        this.linkRepository.deleteAll();
    }

    @Test
    void getAllLinks() throws Exception {
        for (int i = 0; i < 10; i++) {
            Link link = new Link();
            link.setOldUrl("https://example-test.com" + i);
            link.setNewUrl("qwerty" + i);
            link.setStatus(Status.PUBLIC);
            link.setUser(this.user.get());
            this.linkRepository.save(link);
        }

        mockMvc.perform(get("/api/v1/link/all")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].oldUrl").value("https://example-test.com0"))
                .andExpect(jsonPath("$.content[1].oldUrl").value("https://example-test.com1"))
                .andExpect(jsonPath("$.content[2].oldUrl").value("https://example-test.com2"));
    }
}