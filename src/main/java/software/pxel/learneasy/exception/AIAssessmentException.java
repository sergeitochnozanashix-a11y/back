package software.pxel.learneasy.exception;

public class AIAssessmentException extends RuntimeException {
    public AIAssessmentException(String message) {
        super(message);
    }

    public AIAssessmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
