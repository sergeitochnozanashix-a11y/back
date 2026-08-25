package software.pxel.learneasy.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class AuthEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        //Покрывает кейсы, когда запрос не проходит дальше фильтра безопасности
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{ \"message\": \"" + authException.getMessage() + "\", " +
                "\"httpCode\": " + HttpStatus.UNAUTHORIZED.value() + "," +
                "\"timestamp\": \"" + Instant.now().toString() + "\"}");
    }
}
