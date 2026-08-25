package software.pxel.learneasy.feign.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import software.pxel.learneasy.feign.whisper.dto.ErrorResponse;
import software.pxel.learneasy.feign.whisper.exception.WhisperApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public CustomFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = readBody(response);
        ErrorResponse er = tryParse(body);
        String msg = er != null && er.message() != null ? er.message() : "Whisper API error";
        int code = er != null && er.httpCode() != null ? er.httpCode() : status;
        return new WhisperApiException(msg, code, body);
    }

    private ErrorResponse tryParse(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readValue(body, ErrorResponse.class);
        } catch (IOException e) {
            return null;
        }
    }

    private String readBody(Response response) {
        if (response.body() == null) return null;
        try (InputStream is = response.body().asInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
