package software.pxel.learneasy.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.pxel.learneasy.controller.api.AuthApi;
import software.pxel.learneasy.api.dto.common.MessageResponse;
import software.pxel.learneasy.api.dto.auth.AuthRequest;
import software.pxel.learneasy.api.dto.auth.AuthResponse;
import software.pxel.learneasy.api.dto.auth.JwtResponse;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.password.ForgotPasswordRequest;
import software.pxel.learneasy.api.dto.password.ResetPasswordRequest;
import software.pxel.learneasy.api.dto.user.RegistrationResponse;
import software.pxel.learneasy.api.dto.user.ResendCodeRequest;
import software.pxel.learneasy.api.dto.user.VerificationResponse;
import software.pxel.learneasy.api.dto.user.VerifyEmailRequest;
import software.pxel.learneasy.config.security.UserAuthProvider;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.AuthenticationService;
import software.pxel.learneasy.service.PasswordResetService;
import software.pxel.learneasy.service.UserService;

import java.util.Map;

import static software.pxel.learneasy.constants.ApiRoutes.AUTH_URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(AUTH_URI)
public class AuthController implements AuthApi {

    private final UserService userService;
    private final UserAuthProvider userAuthenticationProvider;
    private final PasswordResetService passwordResetService;
    private final AuthenticationService authenticationService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {
        User user = userService.login(authRequest);
        String accessToken = userAuthenticationProvider.createAccessToken(user);
        String refreshToken = userAuthenticationProvider.createRefreshToken(user);
        userAuthenticationProvider.saveRefreshToken(refreshToken, user.getId().toString());
        userAuthenticationProvider.addRefreshTokenToCookie(refreshToken, response);

        AuthResponse authResponse = AuthResponse.builder()
                .username(user.getUsername())
                .accessToken(accessToken)
                .build();
        return ResponseEntity.ok(authResponse);
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegistrationResponse response = authenticationService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/verify-email")
    public ResponseEntity<VerificationResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletResponse response) {
        VerificationResponse verificationResponse = authenticationService.verifyEmail(request);

        userAuthenticationProvider.addRefreshTokenToCookie(verificationResponse.refreshToken(), response);

        return ResponseEntity.ok(verificationResponse);
    }

    @Override
    @PostMapping("/resend-verification-code")
    public ResponseEntity<Map<String, String>> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        authenticationService.resendVerificationCode(request);
        return ResponseEntity.ok(Map.of("message", "The new verification code has been sent."));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(HttpServletResponse response) {
        String refreshToken = userAuthenticationProvider.getRefreshTokenFromCookie();

        if (refreshToken == null || !userAuthenticationProvider.isRefreshTokenValid(refreshToken)) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        String username = userAuthenticationProvider.getUsernameFromToken(refreshToken);
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        String newAccessToken = userAuthenticationProvider.createAccessToken(user);
        String newRefreshToken = userAuthenticationProvider.createRefreshToken(user);

        userAuthenticationProvider.invalidateRefreshToken(refreshToken);
        userAuthenticationProvider.saveRefreshToken(newRefreshToken, user.getId().toString());

        userAuthenticationProvider.addRefreshTokenToCookie(newRefreshToken, response);

        return ResponseEntity.ok(new JwtResponse(newAccessToken));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        String refreshToken = userAuthenticationProvider.getRefreshTokenFromCookie();
        if (refreshToken != null) {
            userAuthenticationProvider.invalidateRefreshToken(refreshToken);
            userAuthenticationProvider.removeRefreshTokenCookie(response);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.createPasswordResetToken(request.email());
        return ResponseEntity.ok(new MessageResponse("Если пользователь с таким email существует, на него будет отправлена инструкция по сбросу пароля."));
    }

    @Override
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Пароль успешно сброшен."));
    }
}
