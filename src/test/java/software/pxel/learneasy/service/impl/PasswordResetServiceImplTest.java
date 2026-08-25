package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.pxel.learneasy.api.dto.password.ResetPasswordRequest;
import software.pxel.learneasy.exception.InvalidTokenException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.EmailService;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService — сброс пароля (реализация на Redis)")
class PasswordResetServiceImplTest {

    private static final String REDIS_TOKEN_TO_EMAIL_PREFIX = "password-reset:token:";
    private static final String REDIS_USER_ID_TO_TOKEN_PREFIX = "password-reset:user:";

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<String> valueCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    @InjectMocks
    private PasswordResetServiceImpl service;

    @BeforeEach
    void setup() throws Exception {
        Field f = PasswordResetServiceImpl.class.getDeclaredField("tokenExpiryMinutes");
        f.setAccessible(true);
        f.setLong(service, 30L);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("createPasswordResetToken(email)")
    class CreateToken {

        @Test
        @DisplayName("Успех: пользователь найден -> токен сохраняется в Redis и отправляется письмо")
        void success_userFound_storesInRedisAndEmails() {
            String email = "user@example.com";
            long userId = 42L;
            User user = new User();
            user.setId(userId);
            user.setEmail(email);

            String userToTokenKey = REDIS_USER_ID_TO_TOKEN_PREFIX + userId;

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(valueOperations.get(userToTokenKey)).thenReturn(null);

            service.createPasswordResetToken(email);

            verify(valueOperations, times(2)).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

            List<String> keys = keyCaptor.getAllValues();
            List<String> values = valueCaptor.getAllValues();
            List<Duration> durations = durationCaptor.getAllValues();

            String tokenToEmailKey = keys.stream().filter(k -> k.startsWith(REDIS_TOKEN_TO_EMAIL_PREFIX)).findFirst().orElse(null);
            assertNotNull(tokenToEmailKey, "Ключ 'token -> email' должен быть сохранен");

            int tokenToEmailIndex = keys.indexOf(tokenToEmailKey);
            assertEquals(email, values.get(tokenToEmailIndex));
            assertEquals(Duration.ofMinutes(30), durations.get(tokenToEmailIndex));

            String token = tokenToEmailKey.substring(REDIS_TOKEN_TO_EMAIL_PREFIX.length());
            assertDoesNotThrow(() -> UUID.fromString(token), "Токен должен быть валидным UUID");

            String userIdToTokenSavedKey = keys.stream().filter(k -> k.startsWith(REDIS_USER_ID_TO_TOKEN_PREFIX)).findFirst().orElse(null);
            assertNotNull(userIdToTokenSavedKey, "Ключ 'userId -> token' должен быть сохранен");
            assertEquals(userToTokenKey, userIdToTokenSavedKey);

            int userIdToTokenIndex = keys.indexOf(userIdToTokenSavedKey);
            assertEquals(token, values.get(userIdToTokenIndex));
            assertEquals(Duration.ofMinutes(30), durations.get(userIdToTokenIndex));

            verify(emailService).sendPasswordResetEmail(eq(email), eq(token));

            verify(valueOperations).get(eq(userToTokenKey));

            verifyNoMoreInteractions(emailService, valueOperations);
        }

        @Test
        @DisplayName("Провал: email не найден -> ничего не делает (кроме warn-лога)")
        void failure_emailNotFound_doesNothing() {
            String email = "ghost@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            service.createPasswordResetToken(email);

            verify(userRepository).findByEmail(email);
            verifyNoInteractions(valueOperations, emailService, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("resetPassword(request)")
    class ResetPassword {

        @Test
        @DisplayName("Провал: токен не найден в Redis (или истёк)")
        void failure_tokenNotFoundOrExpired_throwsException() {
            String token = "bad-token";
            String redisKey = REDIS_TOKEN_TO_EMAIL_PREFIX + token;
            when(valueOperations.get(redisKey)).thenReturn(null);

            ResetPasswordRequest request = new ResetPasswordRequest(token, "NewPassw0rd!");

            InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> service.resetPassword(request));
            assertTrue(ex.getMessage().contains("Токен не найден"));

            verify(valueOperations).get(redisKey);
            verifyNoMoreInteractions(valueOperations);
            verifyNoInteractions(userRepository, passwordEncoder, emailService);
        }

        @Test
        @DisplayName("Провал: токен найден, но пользователь с таким email уже удалён")
        void failure_tokenValid_butUserNotFound_throwsException() {
            String token = "valid-token";
            String email = "deleted.user@example.com";
            String redisKey = REDIS_TOKEN_TO_EMAIL_PREFIX + token;

            when(valueOperations.get(redisKey)).thenReturn(email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            ResetPasswordRequest request = new ResetPasswordRequest(token, "NewPassw0rd!");

            InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> service.resetPassword(request));
            assertTrue(ex.getMessage().contains("Пользователь, связанный с токеном, не найден."));

            verify(valueOperations).get(redisKey);
            verify(userRepository).findByEmail(email);
            verify(redisTemplate, never()).delete(anyString());
        }


        @Test
        @DisplayName("Успех: валидный токен -> пароль кодируется, пользователь сохраняется, токен удаляется из Redis")
        void success_validToken_changesPasswordAndDeletesToken() {
            String token = "valid-token";
            String email = "user@example.com";
            String newPassword = "NewPassword123!";
            String encodedPassword = "ENCODED_PASSWORD";
            String redisKey = REDIS_TOKEN_TO_EMAIL_PREFIX + token;

            User user = new User();
            user.setId(15L);
            user.setEmail(email);

            when(valueOperations.get(redisKey)).thenReturn(email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

            service.resetPassword(new ResetPasswordRequest(token, newPassword));

            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals(encodedPassword, savedUser.getPasswordHash());

            verify(redisTemplate).delete(redisKey);
        }
    }
}
