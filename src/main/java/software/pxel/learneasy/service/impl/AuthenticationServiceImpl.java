package software.pxel.learneasy.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.RegistrationResponse;
import software.pxel.learneasy.api.dto.user.ResendCodeRequest;
import software.pxel.learneasy.api.dto.user.VerificationResponse;
import software.pxel.learneasy.api.dto.user.VerifyEmailRequest;
import software.pxel.learneasy.config.security.UserAuthProvider;
import software.pxel.learneasy.exception.InvalidVerificationCodeException;
import software.pxel.learneasy.exception.RateLimitExceededException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.AuthenticationService;
import software.pxel.learneasy.service.EmailService;
import software.pxel.learneasy.service.RedisVerificationService;
import software.pxel.learneasy.service.UserService;

import java.security.SecureRandom;

import static software.pxel.learneasy.constants.AuthConstants.INVALID_VERIFICATION_RESPONSE;
import static software.pxel.learneasy.constants.AuthConstants.MESSAGE_EMAIL_VERIFICATION;
import static software.pxel.learneasy.constants.AuthConstants.RATE_LIMIT_EX_RESPONSE;
import static software.pxel.learneasy.constants.AuthConstants.USER_EMAIL_NOT_FOUND_EX_RESPONSE;
import static software.pxel.learneasy.constants.AuthConstants.USER_EMAIL_NO_VERIFICATION_EX_RESPONSE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserAuthProvider userAuthProvider;
    private final RedisVerificationService redisVerificationService;
    private final EmailService emailService;

    @Override
    public RegistrationResponse register(RegisterRequest request) {
        User createdUser = userService.register(request);

        String verificationCode = generateVerificationCode();

        redisVerificationService.saveVerificationCode(createdUser.getEmail(), verificationCode);
        redisVerificationService.incrementResendCounterAndCheckLockout(createdUser.getEmail());

        emailService.sendVerificationEmail(createdUser.getEmail(), verificationCode);

        return new RegistrationResponse(MESSAGE_EMAIL_VERIFICATION, createdUser.getEmail());
    }

    @Override
    @Transactional
    public VerificationResponse verifyEmail(VerifyEmailRequest request) {
        String storedCode = redisVerificationService.getVerificationCode(request.email());

        if (storedCode == null || !storedCode.equals(request.code())) {
            throw new InvalidVerificationCodeException(INVALID_VERIFICATION_RESPONSE);
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException(USER_EMAIL_NOT_FOUND_EX_RESPONSE));

        user.setVerified(true);
        userRepository.save(user);

        redisVerificationService.deleteVerificationData(request.email());

        String accessToken = userAuthProvider.createAccessToken(user);
        String refreshToken = userAuthProvider.createRefreshToken(user);

        userAuthProvider.saveRefreshToken(refreshToken, user.getId().toString());

        return VerificationResponse.builder()
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void resendVerificationCode(ResendCodeRequest request) {
        if (redisVerificationService.isEmailLockedOut(request.email())) {
            throw new RateLimitExceededException(RATE_LIMIT_EX_RESPONSE);
        }

        User user = userRepository.findByEmail(request.email())
                .filter(u -> !u.isVerified())
                .orElseThrow(() -> new EntityNotFoundException(USER_EMAIL_NO_VERIFICATION_EX_RESPONSE));

        String newCode = generateVerificationCode();
        redisVerificationService.saveVerificationCode(user.getEmail(), newCode);
        emailService.sendVerificationEmail(user.getEmail(), newCode);
    }

    private String generateVerificationCode() {
        int code = this.secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
