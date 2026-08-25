package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.password.ResetPasswordRequest;

public interface PasswordResetService {

    void createPasswordResetToken(String email);

    void resetPassword(ResetPasswordRequest request);
}
