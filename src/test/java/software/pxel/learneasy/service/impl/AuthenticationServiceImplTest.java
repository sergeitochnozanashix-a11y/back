package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.RegistrationResponse;
import software.pxel.learneasy.config.security.UserAuthProvider;
import software.pxel.learneasy.exception.EmailDeliveryException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.model.enums.UserRole;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.EmailService;
import software.pxel.learneasy.service.RedisVerificationService;
import software.pxel.learneasy.service.UserService;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthenticationService — регистрация и доставка кода")
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuthProvider userAuthProvider;
    @Mock
    private RedisVerificationService redisVerificationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationServiceImpl service;

    private static final String EMAIL = "new.user@example.com";

    private static User user() {
        User user = new User("newuser", EMAIL, "hash", UserRole.USER);
        user.setId(42L);
        return user;
    }

    private static RegisterRequest request() {
        return new RegisterRequest("newuser", EMAIL, "password");
    }

    @Nested
    @DisplayName("register(request)")
    class Register {

        @Test
        @DisplayName("успех — код отправлен, emailSent = true")
        void success_codeDelivered() {
            when(userService.register(any(RegisterRequest.class))).thenReturn(user());

            RegistrationResponse response = service.register(request());

            assertAll(
                    () -> assertEquals(EMAIL, response.email()),
                    () -> assertTrue(response.emailSent())
            );
            verify(emailService).sendVerificationEmail(anyString(), anyString());
            verify(redisVerificationService).saveVerificationCode(anyString(), anyString());
        }

        @Test
        @DisplayName("сбой почты не отменяет регистрацию — аккаунт создан, emailSent = false")
        void emailFailure_stillRegisters() {
            when(userService.register(any(RegisterRequest.class))).thenReturn(user());
            doThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                    .when(emailService).sendVerificationEmail(anyString(), anyString());

            // Ключевой сценарий: раньше исключение уходило наружу как 500, хотя
            // пользователь был уже закоммичен. Клиент оставался в тупике -
            // повторная регистрация 409, вход 401, экран с повторной отправкой
            // кода недостижим.
            RegistrationResponse response = service.register(request());

            assertAll(
                    () -> assertEquals(EMAIL, response.email()),
                    () -> assertFalse(response.emailSent()),
                    () -> assertTrue(response.message().toLowerCase().contains("again"), response.message())
            );
        }

        @Test
        @DisplayName("недоступность Redis тоже не отменяет регистрацию")
        void redisFailure_stillRegisters() {
            when(userService.register(any(RegisterRequest.class))).thenReturn(user());
            doThrow(new RuntimeException("redis unavailable"))
                    .when(redisVerificationService).saveVerificationCode(anyString(), anyString());

            RegistrationResponse response = service.register(request());

            assertFalse(response.emailSent());
            verify(emailService, org.mockito.Mockito.never()).sendVerificationEmail(anyString(), anyString());
        }
    }
}
