package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.RegistrationResponse;
import software.pxel.learneasy.api.dto.user.ResendCodeRequest;
import software.pxel.learneasy.api.dto.user.VerificationResponse;
import software.pxel.learneasy.api.dto.user.VerifyEmailRequest;

public interface AuthenticationService {

    RegistrationResponse register(RegisterRequest request);

    VerificationResponse verifyEmail(VerifyEmailRequest request);

    void resendVerificationCode(ResendCodeRequest request);
}
