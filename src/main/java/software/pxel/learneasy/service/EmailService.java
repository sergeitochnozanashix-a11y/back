package software.pxel.learneasy.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String token);

    void sendVerificationEmail(String toEmail, String verificationCode);
}
