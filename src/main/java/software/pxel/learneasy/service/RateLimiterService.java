package software.pxel.learneasy.service;

public interface RateLimiterService {

    void checkUserLimit(Long userId);
}
