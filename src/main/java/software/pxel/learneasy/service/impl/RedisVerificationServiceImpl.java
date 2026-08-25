package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.service.RedisVerificationService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisVerificationServiceImpl implements RedisVerificationService {

    private final StringRedisTemplate redisTemplate;

    private static final String VERIFICATION_CODE_PREFIX = "verify:code:";
    private static final String RESEND_COUNTER_PREFIX = "verify:resend_count:";
    private static final String LOCKOUT_PREFIX = "verify:lockout:";

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration LOCKOUT_TTL = Duration.ofHours(2);
    private static final int MAX_RESEND_ATTEMPTS = 5;


    @Override
    public void saveVerificationCode(String email, String code) {
        redisTemplate.opsForValue().set(VERIFICATION_CODE_PREFIX + email, code, CODE_TTL);
    }

    @Override
    public String getVerificationCode(String email) {
        return redisTemplate.opsForValue().get(VERIFICATION_CODE_PREFIX + email);
    }

    @Override
    public boolean isEmailLockedOut(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_PREFIX + email));
    }

    @Override
    public void incrementResendCounterAndCheckLockout(String email) {
        String countKey = RESEND_COUNTER_PREFIX + email;
        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        if (currentCount == 1) {
            redisTemplate.expire(countKey, LOCKOUT_TTL);
        }

        if (currentCount != null && currentCount >= MAX_RESEND_ATTEMPTS) {
            redisTemplate.opsForValue().set(LOCKOUT_PREFIX + email, "locked", LOCKOUT_TTL);
        }
    }

    @Override
    public void deleteVerificationData(String email) {
        redisTemplate.delete(VERIFICATION_CODE_PREFIX + email);
        redisTemplate.delete(RESEND_COUNTER_PREFIX + email);
    }
}
