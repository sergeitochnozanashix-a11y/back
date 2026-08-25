package software.pxel.learneasy.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.UserService;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class UserAuthProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.access}")
    private Long accessExpirationMillis;

    @Value("${jwt.expiration.refresh}")
    private Long refreshExpirationMillis;

    @Value("${jwt.refreshToken.cookie-name}")
    private String refreshTokenCookieName;

    private final UserService userService;

    private Algorithm algorithm;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "refresh_token:";

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        algorithm = Algorithm.HMAC256(secretKey);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(Duration.ofMillis(accessExpirationMillis));

        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(validity))
                .withClaim("role", user.getRole().name())
                .sign(algorithm);
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(Duration.ofMillis(refreshExpirationMillis));

        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(validity))
                .withClaim("role", user.getRole().name())
                .sign(algorithm);
    }

    public Authentication validateToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        User user = userService.findByUsername(jwt.getSubject());
        return new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(user.getRole()));
    }

    public void saveRefreshToken(String token, String userId) {
        redisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + token,
                userId,
                Duration.ofMillis(refreshExpirationMillis)
        );
    }

    private DecodedJWT verifyAndDecodeToken(String token) throws JWTVerificationException {
        return JWT.require(algorithm).build().verify(token);
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            DecodedJWT jwt = verifyAndDecodeToken(token);
            return redisTemplate.hasKey(REDIS_KEY_PREFIX + token) &&
                    jwt.getExpiresAt().after(new Date());
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    public void invalidateRefreshToken(String token) {
        redisTemplate.delete(REDIS_KEY_PREFIX + token);
    }

    public String getUsernameFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            return jwt.getSubject();
        } catch (JWTVerificationException ex) {
            throw new JwtAuthenticationException("Invalid token: " + ex.getMessage());
        }
    }

    public void addRefreshTokenToCookie(String token, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(refreshExpirationMillis))
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getRefreshTokenFromCookie() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public User getByUsername(String username) {
        return userService.findByUsername(username);
    }

    public User getCurrentUser() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getByUsername(username);
    }
}
