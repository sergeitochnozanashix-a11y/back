package software.pxel.learneasy.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.exception.RateLimitExceededException;
import software.pxel.learneasy.service.RateLimiterService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final String RATE_LIMIT_EX_RESPONSE = "Request limit exceeded: only 30 messages/hour allowed";

    private final ProxyManager<String> proxyManager;

    /**
     * Проверяет лимит 30 запросов в час для конкретного userId.
     * Если токенов нет – кидает RateLimitExceededException.
     */
    public void checkUserLimit(Long userId) {
        String key = "rate-limit:user:" + userId;

        Bucket bucket = proxyManager.builder().build(
                key,
                () -> BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(30, Duration.ofHours(1)))
                        .build()
        );

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(RATE_LIMIT_EX_RESPONSE);
        }
    }
}
