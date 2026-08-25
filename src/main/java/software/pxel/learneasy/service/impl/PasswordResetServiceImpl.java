package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.api.dto.password.ResetPasswordRequest;
import software.pxel.learneasy.exception.InvalidTokenException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.EmailService;
import software.pxel.learneasy.service.PasswordResetService;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${password.reset.token-expiry-minutes:30}")
    private long tokenExpiryMinutes;

    private static final String REDIS_TOKEN_TO_EMAIL_PREFIX = "password-reset:token:";
    private static final String REDIS_USER_ID_TO_TOKEN_PREFIX = "password-reset:user:";

    @Override
    public void createPasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        userOptional.ifPresent(user -> {
            invalidateOldTokenIfPresent(user.getId());

            String newToken = UUID.randomUUID().toString();
            String tokenToEmailKey = REDIS_TOKEN_TO_EMAIL_PREFIX + newToken;
            String userIdToTokenKey = REDIS_USER_ID_TO_TOKEN_PREFIX + user.getId();
            Duration tokenTtl = Duration.ofMinutes(tokenExpiryMinutes);

            redisTemplate.opsForValue().set(tokenToEmailKey, user.getEmail(), tokenTtl);
            redisTemplate.opsForValue().set(userIdToTokenKey, newToken, tokenTtl);

            emailService.sendPasswordResetEmail(user.getEmail(), newToken);
            log.info("Новый токен сброса пароля создан и отправлен для пользователя с ID: {}", user.getId());
        });

        if (userOptional.isEmpty()) {
            log.warn("Попытка сброса пароля для несуществующего email: {}", email);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String tokenToEmailKey = REDIS_TOKEN_TO_EMAIL_PREFIX + request.token();
        String email = redisTemplate.opsForValue().get(tokenToEmailKey);

        if (email == null) {
            throw new InvalidTokenException("Токен не найден, недействителен или истек.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Пользователь, связанный с токеном, не найден."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        String userIdToTokenKey = REDIS_USER_ID_TO_TOKEN_PREFIX + user.getId();
        redisTemplate.delete(tokenToEmailKey);
        redisTemplate.delete(userIdToTokenKey);

        log.info("Пароль пользователя с ID: {} успешно сброшен.", user.getId());
    }

    private void invalidateOldTokenIfPresent(Long userId) {
        String userIdToTokenKey = REDIS_USER_ID_TO_TOKEN_PREFIX + userId;
        String oldToken = redisTemplate.opsForValue().get(userIdToTokenKey);

        if (oldToken != null) {
            String oldTokenToEmailKey = REDIS_TOKEN_TO_EMAIL_PREFIX + oldToken;
            redisTemplate.delete(oldTokenToEmailKey);
            redisTemplate.delete(userIdToTokenKey);
            log.info("Старый токен сброса пароля для пользователя с ID: {} был инвалидирован.", userId);
        }
    }
}
