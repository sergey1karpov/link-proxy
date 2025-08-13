package com.linker.linker.service;

import com.linker.linker.entity.User;
import com.linker.linker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void updateUserProfile(User mappedUser, MultipartFile file) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        user.setEmail(mappedUser.getEmail() != null ? mappedUser.getEmail() : user.getEmail());
        user.setAvatar(this.saveImageHelper(file));

        this.userRepository.save(user);
    }

    private String saveImageHelper(MultipartFile file) throws IOException {
        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        file.transferTo(filePath);

        return "/files/" + fileName;
    }
}
