package software.pxel.learneasy.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;
import software.pxel.learneasy.exception.EmailDeliveryException;
import software.pxel.learneasy.service.EmailService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.reset-password-url}")
    private String frontendResetPasswordUrl;

    private String passwordResetEmailTemplate;
    private String verificationEmailTemplate;

    private static final String RESET_URL_PLACEHOLDER = "[[RESET_URL]]";
    private static final String VERIFICATION_CODE_PLACEHOLDER = "[[VERIFICATION_CODE]]";

    private static final String PASSWORD_RESET_TEMPLATE_PATH = "templates/password-reset-email.html";
    private static final String EMAIL_VERIFICATION_TEMPLATE_PATH = "templates/email-verification.html";

    /**
     * Инициализирует шаблон электронного письма, загружая его из файла classpath.
     * Этот метод вызывается после завершения внедрения зависимостей.
     */
    @PostConstruct
    public void loadEmailTemplate() {
        passwordResetEmailTemplate = loadTemplate(PASSWORD_RESET_TEMPLATE_PATH);
        verificationEmailTemplate = loadTemplate(EMAIL_VERIFICATION_TEMPLATE_PATH);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        var subject = "Сброс пароля для вашего аккаунта";

        if (passwordResetEmailTemplate == null || passwordResetEmailTemplate.isBlank()) {
            log.error("Password reset email template is not loaded. Cannot send email to {}.", to);
            throw new IllegalStateException("Email template is not loaded: " + PASSWORD_RESET_TEMPLATE_PATH);
        }

        try {
            String resetUrl = frontendResetPasswordUrl + "?token=" + token;
            String htmlContent = passwordResetEmailTemplate.replace(RESET_URL_PLACEHOLDER, resetUrl);

            sendMimeMessage(to, subject, htmlContent);
            log.info("Письмо для сброса пароля отправлено на: {}", to);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке письма для сброса пароля на {}", to, e);
        }
    }

    @Override
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        var subject = "Подтверждение вашего email";

        if (verificationEmailTemplate == null || verificationEmailTemplate.isBlank()) {
            log.error("Verification email template is not loaded. Cannot send email to {}.", toEmail);
            throw new IllegalStateException("Email template is not loaded: " + EMAIL_VERIFICATION_TEMPLATE_PATH);
        }

        try {
            String htmlContent = verificationEmailTemplate.replace(VERIFICATION_CODE_PLACEHOLDER, verificationCode);

            sendMimeMessage(toEmail, subject, htmlContent);
            log.info("Письмо с кодом верификации отправлено на: {}", toEmail);
        } catch (MessagingException | MailException e) {
            // Раньше исключение здесь гасилось, и вызывающий код считал письмо
            // отправленным. Пробрасываем, чтобы register мог честно сообщить
            // emailSent=false, а повторная отправка не отвечала ложным успехом.
            //
            // MailException ловим отдельно: сбои соединения и аутентификации
            // SMTP приходят именно в ней, она не наследует MessagingException и
            // раньше улетала наружу нетронутой - клиент видел безликое 500.
            log.error("Ошибка при отправке письма для верификации на {}", toEmail, e);
            throw new EmailDeliveryException("Failed to send verification email to " + toEmail, e);
        }
    }

    /**
     * Вспомогательный приватный метод для загрузки шаблона из файла.
     * Избегает дублирования кода.
     */
    private String loadTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String template = reader.lines().collect(Collectors.joining("\n"));
                log.info("Email template '{}' loaded successfully.", templatePath);
                return template;
            }
        } catch (IOException e) {
            log.error("Failed to load email template from '{}'. Application may not function correctly.", templatePath, e);
            throw new IllegalStateException("Failed to load email template: " + templatePath, e);
        }
    }

    protected void sendMimeMessage(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        buildAndSendMessage(helper, to, subject, htmlContent, mimeMessage);
    }

    protected void buildAndSendMessage(MimeMessageHelper helper, String to, String subject, String htmlContent, MimeMessage mimeMessage) throws MessagingException {
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom("no-reply@educationalproject.com");
        helper.setText(htmlContent, true);
        mailSender.send(mimeMessage);
    }
}
