package com.linker.linker.service.auth;

import com.linker.linker.dto.auth.LoginRequestDto;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.entity.User;
import com.linker.linker.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Transactional
    public User registerUser(User mappedUser) {
        User user = User.builder()
                .username(mappedUser.getUsername())
                .email(mappedUser.getEmail())
                .password(encoder.encode(mappedUser.getPassword()))
                .role(User.Role.ROLE_USER)
                .build();

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Проверяет, существуют ли уже в базе указанные email и username,
     * а также обрабатывает валидационные ошибки из {@link RegisterRequestDto} при регистрации.
     *
     * @param email    email пользователя для проверки
     * @param username имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    public List<String> registerValidate(String email, String username, BindingResult bindingResult) {
        if(this.userRepository.findByEmail(email).isPresent()) {
            bindingResult.rejectValue("email", "", "Email already exists");
        }
        if(this.userRepository.findByUsername(username).isPresent()) {
            bindingResult.rejectValue("username", "", "Username already exists");
        }

        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, существуют ли в базе юзер с указанным username,
     * а также обрабатывает валидационные ошибки из {@link LoginRequestDto} при логине.
     *
     * @param username имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    public List<String> loginValidate(String username, BindingResult bindingResult) {
        if(this.userRepository.findByUsername(username).isEmpty()) {
            bindingResult.rejectValue("username", "", "User not found");
        }

        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
    }
}
