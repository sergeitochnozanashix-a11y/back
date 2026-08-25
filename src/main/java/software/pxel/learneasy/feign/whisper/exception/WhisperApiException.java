package software.pxel.learneasy.feign.whisper.exception;

public class WhisperApiException extends RuntimeException {
    private final int httpCode;
    private final String payload;

    public WhisperApiException(String message, int httpCode, String payload) {
        super(message);
        this.httpCode = httpCode;
        this.payload = payload;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getPayload() {
        return payload;
    }
}
