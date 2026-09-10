package software.pxel.learneasy.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import software.pxel.learneasy.exception.EmailDeliveryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Spy
    @InjectMocks
    private EmailServiceImpl service;

    @Captor
    private ArgumentCaptor<String> toCaptor;
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> htmlContentCaptor;

    private static final String MOCKED_PASSWORD_RESET_TEMPLATE = "<html><body><a href=[[RESET_URL]]>Reset</a></body></html>";
    private static final String MOCKED_VERIFICATION_TEMPLATE = "<html><body>Код: [[VERIFICATION_CODE]]</body></html>";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendResetPasswordUrl", "https://front.example/reset");
        ReflectionTestUtils.setField(service, "passwordResetEmailTemplate", MOCKED_PASSWORD_RESET_TEMPLATE);
        ReflectionTestUtils.setField(service, "verificationEmailTemplate", MOCKED_VERIFICATION_TEMPLATE);
    }

    @Nested
    @DisplayName("Отправка письма для сброса пароля")
    class PasswordReset {

        @Test
        @DisplayName("Успешный сценарий: вызывает sendMimeMessage с правильными параметрами")
        void sendPasswordResetEmail_ok() throws Exception {
            String to = "user@example.com";
            String token = "abc123";

            doNothing().when(service).sendMimeMessage(toCaptor.capture(), subjectCaptor.capture(), htmlContentCaptor.capture());

            service.sendPasswordResetEmail(to, token);

            assertEquals(to, toCaptor.getValue());
            assertEquals("Сброс пароля для вашего аккаунта", subjectCaptor.getValue());

            String capturedHtml = htmlContentCaptor.getValue();
            String expectedUrl = "https://front.example/reset?token=" + token;
            assertTrue(capturedHtml.contains(expectedUrl));
            assertFalse(capturedHtml.contains("[[RESET_URL]]"));
        }

        @Test
        @DisplayName("Сценарий ошибки: выбрасывает исключение, если шаблон не загружен")
        void sendPasswordResetEmail_templateNotLoaded_throwsException() {
            ReflectionTestUtils.setField(service, "passwordResetEmailTemplate", null);

            assertThrows(IllegalStateException.class,
                    () -> service.sendPasswordResetEmail("user@example.com", "abc123"));

            verify(mailSender, never()).send((MimeMessage) any());
        }
    }

    @Nested
    @DisplayName("Отправка письма для верификации email")
    class EmailVerification {

        @Test
        @DisplayName("Успешный сценарий: вызывает sendMimeMessage с правильными параметрами")
        void sendVerificationEmail_ok() throws Exception {
            String to = "new.user@example.com";
            String code = "123456";

            doNothing().when(service).sendMimeMessage(toCaptor.capture(), subjectCaptor.capture(), htmlContentCaptor.capture());

            service.sendVerificationEmail(to, code);

            assertEquals(to, toCaptor.getValue());
            assertEquals("Подтверждение вашего email", subjectCaptor.getValue());

            String capturedHtml = htmlContentCaptor.getValue();
            assertTrue(capturedHtml.contains(code));
            assertFalse(capturedHtml.contains("[[VERIFICATION_CODE]]"));
        }

        @Test
        @DisplayName("Сценарий ошибки: сбой отправки пробрасывается, а не гасится")
        void sendVerificationEmail_messagingFailure_propagates() throws Exception {
            doThrow(new MessagingException("smtp down"))
                    .when(service).sendMimeMessage(any(), any(), any());

            // Раньше исключение здесь логировалось и гасилось: вызывающий код
            // не мог отличить отправленное письмо от неотправленного.
            assertThrows(EmailDeliveryException.class,
                    () -> service.sendVerificationEmail("new.user@example.com", "123456"));
        }

        @Test
        @DisplayName("Сценарий ошибки: выбрасывает исключение, если шаблон не загружен")
        void sendVerificationEmail_templateNotLoaded_throwsException() {
            ReflectionTestUtils.setField(service, "verificationEmailTemplate", null);

            assertThrows(IllegalStateException.class,
                    () -> service.sendVerificationEmail("new.user@example.com", "123456"));

            verify(mailSender, never()).send((MimeMessage) any());
        }
    }
}
