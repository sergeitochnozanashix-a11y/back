package software.pxel.learneasy.service;

public interface RedisVerificationService {

    void saveVerificationCode(String email, String code);

    String getVerificationCode(String email);

    boolean isEmailLockedOut(String email);

    void incrementResendCounterAndCheckLockout(String email);

    void deleteVerificationData(String email);
}
