package com.linker.linker.service.auth;

import com.linker.linker.dto.auth.LoginRequestDto;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.dto.auth.ManualPasswordChangeRequestDto;
import com.linker.linker.dto.auth.UserIdAndSecretCode;
import com.linker.linker.entity.User;
import com.linker.linker.entity.utils.Role;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User mappedUser) {
        User user = new User();
        user.setUsername(mappedUser.getUsername());
        user.setEmail(mappedUser.getEmail());
        user.setPassword(encoder.encode(mappedUser.getPassword()));
        user.setRole(Role.ROLE_USER);

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Optional<User> getUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
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
    public List<String> loginValidate(String username, String password, BindingResult bindingResult) {
        Optional<User> user = this.userRepository.findByUsername(username);

        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
            bindingResult.rejectValue("username", "", "User not found or password incorrect");
        }

        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, существуют ли в базе юзер с указанным email,
     * а также обрабатывает валидационные ошибки из {@link ManualPasswordChangeRequestDto} при логине.
     *
     * @param email имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    public List<String> emailValidate(String email, BindingResult bindingResult) {
        if(this.userRepository.findByEmail(email).isEmpty()) {
            bindingResult.rejectValue("email", "", "User with this email not found");
        }

        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
    }

    /**
     * Вставляем в таблицу manual_password_change данные с запросом на
     * ручную смену пароля
     *
     * @param email вставляем по почте
     * @return hash, который будет отправлен пользователю
     */
    @Transactional
    public String insertIntoResetTable(String email) {
        String hash = UUID.randomUUID().toString();
        this.userRepository.insertIntoResetTable(email, hash, LocalDateTime.now());
        return hash;
    }

    /**
     * Ограничитель на запросы по изменению пароля,
     * пользователь имеет возможность оправить 1 запрос в 5 минут
     * иначе получит валидационную ошибку с просьбой подождать несколько минут
     *
     * @param email имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     */
    public void checkThrottle(String email, BindingResult bindingResult) {
        Optional<LocalDateTime> lastTime = this.userRepository.getCreatedAtTimeToResetPass(email);

        if (lastTime.isEmpty()) return;

        long minutesPassed = checkTime(lastTime);

        if (minutesPassed < 5) {
            bindingResult.rejectValue("email", "", "Wait 5 minutes before new attempt.");
        }
    }

    /**
     * Проверка срока жизни хеша, 10 минут
     *
     * @param hash имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     */
    public void hashValidate(String hash, BindingResult bindingResult) {
        Optional<LocalDateTime> lastTime = this.userRepository.getCreatedAtTimeToUpdatePass(hash);

        if (lastTime.isEmpty()) return;

        long minutesPassed = checkTime(lastTime);

        if (minutesPassed > 10) {
            bindingResult.rejectValue("error", "", "Hash not walid");
        }
    }

    /**
     * Получаем разницу в минутах из поля created_at в таблице manual_password_change
     * для методов {@link #checkThrottle(String, BindingResult)} и {@link #hashValidate(String, BindingResult)}
     *
     * @param lastTime время когда была сделана запись с хешем
     * @return разница во времени в минутах
     */
    public long checkTime(Optional<LocalDateTime> lastTime) {
        LocalDateTime createdAt = lastTime.get();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        return duration.toMinutes();
    }

    /**
     * Сравниваем старый пользовательский пароль с новым, если пароли не совпадают
     * то отдаем клиенту валидационную ошибку
     *
     * @param hash получаем юзера по хешу
     * @param oldPassword старый пароль
     * @param bindingResult для ошибок валидации
     */
    public void compareOldUserPassword(String hash, String oldPassword, BindingResult bindingResult) {
        Optional<User> user = this.userRepository.getUserByHash(hash);
        boolean matches = this.passwordEncoder.matches(oldPassword, user.get().getPassword());
        if (!matches) bindingResult.rejectValue("error", "", "PASSWORD NOT MATCHED");
    }

    /**
     * Меняем старый пароль на новый
     *
     * @param hash достаем юзера по хешу
     * @param newPassword новый пароль
     */
    @Transactional
    public void replaceOldPassword(String hash, String newPassword) {
        Optional<User> user = this.userRepository.getUserByHash(hash);
        user.get().setPassword(encoder.encode(newPassword));
        this.userRepository.save(user.get());
    }

    public int generateResetPasswordSecretCode() {
        Random random = new Random();
        return random.nextInt(999999) + 1;
    }

    public void saveSecretCode(long userId, String email, int secretCode, String hash) {
        this.userRepository.saveSecretCode(
            email, String.valueOf(secretCode), LocalDateTime.now(), userId, hash
        );
    }

    public Optional<User> getChangedUser(String hash, String secretCode, BindingResult bindingResult) {
        UserIdAndSecretCode userData = this.userRepository.getUserIdAndSecretCode(hash);

        if(!userData.getSecretCode().equals(secretCode)) {
            bindingResult.rejectValue("error", "", "code not matched");
        }

        return this.userRepository.findById(userData.getUserId());
    }

    public String autoChangePassword(User user) {
        String newPass = UUID.randomUUID().toString();

        user.setPassword(encoder.encode(newPass));
        this.userRepository.save(user);

        return newPass;
    }
}
