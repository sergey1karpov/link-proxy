package com.linker.linker.service.auth;

import com.linker.linker.dto.auth.LoginRequestDto;
import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.dto.auth.ManualPasswordChangeRequestDto;
import com.linker.linker.entity.User;
import com.linker.linker.entity.utils.ResetType;
import com.linker.linker.entity.utils.Role;
import com.linker.linker.exception.UserNotFoundException;
import com.linker.linker.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Optional<User> getUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    /**
     * Регистрирует пользователя в системе
     * @param mappedUser маппинг данных из {@link RegisterRequestDto}
     * @return возвращает сохраненного юзера в бд
     */
    @Transactional
    public User registerUser(User mappedUser) {
        User user = new User();
        user.setUsername(mappedUser.getUsername());
        user.setEmail(mappedUser.getEmail());
        user.setPassword(encoder.encode(mappedUser.getPassword()));
        user.setRole(Role.ROLE_USER);

        return userRepository.save(user);
    }

    /**
     * Проверяет, существуют ли уже в базе указанные email и username,
     * а также обрабатывает валидационные ошибки из {@link RegisterRequestDto} при регистрации.
     *
     * @param email         email пользователя для проверки
     * @param username      имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    @Transactional(readOnly = true)
    public Map<String, String> registerValidate(String email, String username, BindingResult bindingResult) {
        if (this.userRepository.findByEmail(email).isPresent()) {
            bindingResult.rejectValue("email", "", "Email already exists");
        }

        if (this.userRepository.findByUsername(username).isPresent()) {
            bindingResult.rejectValue("username", "", "Username already exists");
        }

        return validationHandler(bindingResult);
    }

    /**
     * Проверяет, существуют ли в базе юзер с указанным username,
     * а также обрабатывает валидационные ошибки из {@link LoginRequestDto} при логине.
     *
     * @param username      имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    @Transactional(readOnly = true)
    public Map<String, String> loginValidate(String username, String password, BindingResult bindingResult) {
        Optional<User> user = this.userRepository.findByUsername(username);

        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
            bindingResult.rejectValue("username", "", "User not found or password incorrect");
        }

        return validationHandler(bindingResult);
    }

    /**
     * Проверяет, существуют ли в базе юзер с указанным email,
     * а также обрабатывает валидационные ошибки из {@link ManualPasswordChangeRequestDto} при логине.
     *
     * @param email имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     * @return список сообщений об ошибках, если таковые есть
     */
    @Transactional(readOnly = true)
    public Map<String, String> emailValidate(String email, BindingResult bindingResult) {
        if(this.userRepository.findByEmail(email).isEmpty()) {
            bindingResult.rejectValue("email", "", "User with this email not found");
        }

        return validationHandler(bindingResult);
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
     * Ограничитель на запросы по изменению пароля.
     * Пользователь имеет возможность оправить 1 запрос в 5 минут на конкретный email,
     * иначе получит валидационную ошибку с просьбой подождать несколько минут
     *
     * @param email имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     */
    @Transactional(readOnly = true)
    public void checkCountOfAttempts(String email, BindingResult bindingResult, ResetType resetType) {
        LocalDateTime lastTime = switch (resetType) {
            case ResetType.AUTO-> userRepository.getCreatedAtTimeToAutoResetPass(email);
            case ResetType.MANUAL -> userRepository.getCreatedAtTimeToResetPass(email);
        };

        if (lastTime == null) return;

        if (checkTime(lastTime) < 5) {
            bindingResult.rejectValue("email", "", "Wait 5 minutes before new attempt.");
        }
    }

    /**
     * Проверка срока жизни хеша, 10 минут
     *
     * @param hash имя пользователя для проверки
     * @param bindingResult {@link org.springframework.validation.BindingResult} для накопления ошибок
     */
    @Transactional(readOnly = true)
    public void hashValidate(String hash, BindingResult bindingResult, ResetType resetType) {
        LocalDateTime lastTime = switch (resetType) {
            case ResetType.AUTO-> userRepository.getCreatedAtTimeToAutoUpdatePass(hash);
            case ResetType.MANUAL -> this.userRepository.getCreatedAtTimeToUpdatePass(hash);
        };

        if (lastTime == null) return;

        if (checkTime(lastTime) > 10) {
            bindingResult.rejectValue("error", "", "Hash not walid");
        }
    }

    /**
     * Получаем разницу в минутах из поля created_at в таблице manual_password_change
     * для методов {@link #checkCountOfAttempts(String, BindingResult, ResetType)}
     * и {@link #hashValidate(String, BindingResult, ResetType)}
     *
     * @param lastTime время когда была сделана запись с хешем
     * @return разница во времени в минутах
     */
    public long checkTime(LocalDateTime lastTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastTime, now);

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
    @Transactional(readOnly = true)
    public Map<String, String> compareOldUserPassword(String hash, String oldPassword, BindingResult bindingResult) {
        Optional<User> user = this.userRepository.getUserByHash(hash);
        if (user.isEmpty()) throw new UserNotFoundException("User not found");

        boolean matches = this.passwordEncoder.matches(oldPassword, user.get().getPassword());

        if (!matches) {
            bindingResult.rejectValue("error", "", "PASSWORD NOT MATCHED");
        }

        return validationHandler(bindingResult);
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
        if (user.isEmpty()) throw new UserNotFoundException("User not found");

        user.get().setPassword(encoder.encode(newPassword));
        this.userRepository.save(user.get());
    }

    /**
     * Генерируем код для смены пароля
     * @return сгенерированный код
     */
    public int generateResetPasswordSecretCode() {
        Random random = new Random();
        return random.nextInt(999999) + 1;
    }

    /**
     * Сохраняем заявку на автоматическую смену пароля
     * @param email email
     * @return secretCode и hash
     */
    @Transactional
    public Map<String, String> saveSecretCode(String email) {
        int secretCode = this.generateResetPasswordSecretCode();
        String hash = UUID.randomUUID().toString();

        Optional<User> user = this.getUserByEmail(email);
        if (user.isEmpty()) throw new UserNotFoundException("User not found");

        this.userRepository.saveSecretCode(
            email, String.valueOf(secretCode), LocalDateTime.now(), user.get().getId(), hash
        );

        return Map.of(
                "secretCode", String.valueOf(secretCode),
                "hash", hash
        );
    }

    /**
     * Получаем юзера по хешу, user_id из таблицы auto_password_change
     * и по полученному id достаем юзера из users
     * @param hash hash
     * @return user
     */
    @Transactional(readOnly = true)
    public Optional<User> getChangedUser(String hash) {
        long userId = this.userRepository.getUserId(hash);

        return this.userRepository.findById(userId);
    }

    /**
     * Сравниваем код, который получили по почте с секретным кодом в бд
     * @param hash по hash достаем секретный код из auto_password_change
     * @param secretCode введенный пользователем код
     * @param bindingResult для ошибок
     * @return map с ошибками валидации, если таковые есть
     */
    @Transactional(readOnly = true)
    public Map<String, String> compareSecretCode(String hash, String secretCode, BindingResult bindingResult) {
        String code = this.userRepository.getUserSecretCode(hash);

        if(!code.equals(secretCode)) {
            bindingResult.rejectValue("error", "", "code not matched");
        }

        return validationHandler(bindingResult);
    }

    /**
     * Генерируем новый пароль и меняем его в бд
     * @param user user
     * @return новый пароль
     */
    @Transactional
    public String autoChangePassword(User user) {
        String newPass = UUID.randomUUID().toString();

        user.setPassword(encoder.encode(newPass));
        this.userRepository.save(user);

        return newPass;
    }

    /**
     * Обрабатываем ошибки валидации
     * @param bindingResult {@link BindingResult}
     * @return map с ошибками валидации
     */
    private Map<String, String> validationHandler(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> Optional.ofNullable(err.getDefaultMessage()).orElse("Invalid value"),
                        (existing, replacement) -> existing // если дублируются — берем первый
                ));
    }
}
