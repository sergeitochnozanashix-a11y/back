package software.pxel.learneasy.exception.global;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.exception.AIAssessmentException;
import software.pxel.learneasy.exception.AIIntegrationException;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.InvalidTokenException;
import software.pxel.learneasy.exception.InvalidVerificationCodeException;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.exception.RateLimitExceededException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.StorageException;
import software.pxel.learneasy.exception.UserAlreadyExistsException;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse createErrorResponse(String message, HttpStatus status) {
        return new ErrorResponse(message, status.value(), Instant.now().toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors, ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Validation failed: " + errors, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException ex) {
        log.warn("JWT Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Void> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("Access Denied: " + ex.getMessage(), HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("User with username '" + ex.getMessage() + "' not found.", HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Request body could not be read: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(describeUnreadableBody(ex), HttpStatus.BAD_REQUEST));
    }

    /**
     * Jackson не может собрать объект - например, в поле-перечисление пришло
     * значение вне набора - и падает ещё до bean validation, поэтому такие
     * ошибки не попадают в MethodArgumentNotValidException и раньше сваливались
     * в безликое "Json parse error". Достаём из причины имя поля и допустимые
     * значения, чтобы сообщение было в одном ряду с остальными проверками.
     */
    private String describeUnreadableBody(HttpMessageNotReadableException ex) {
        if (!(ex.getCause() instanceof InvalidFormatException cause)) {
            return "Некорректный JSON в теле запроса.";
        }

        String field = cause.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."));
        if (field.isEmpty()) {
            field = "тело запроса";
        }

        Class<?> targetType = cause.getTargetType();
        if (targetType != null && targetType.isEnum()) {
            String allowed = Arrays.stream(targetType.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return "Validation failed: " + field + ": недопустимое значение '" + cause.getValue()
                    + "'. Допустимые значения: " + allowed + ".";
        }

        String typeName = targetType != null ? targetType.getSimpleName() : "ожидаемый тип";
        return "Validation failed: " + field + ": значение '" + cause.getValue()
                + "' не соответствует типу " + typeName + ".";
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Storage operation failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflictException(ResourceConflictException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("File too large: {}", exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(createErrorResponse("File size exceeds the maximum limit", HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException exc) {
        log.warn("Request limit exceeded: {}", exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse(exc.getMessage(), HttpStatus.TOO_MANY_REQUESTS));
    }

    @ExceptionHandler(AIIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleAIIntegrationException(AIIntegrationException exc) {
        log.error("Failed to get response from AI service: {}", exc.getMessage(), exc);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(createErrorResponse("An error occurred while communicating with the ProxyAPI service.", HttpStatus.BAD_GATEWAY));
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCodeException(InvalidVerificationCodeException exc) {
        log.warn("Invalid verification code provided: {}", exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(exc.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException exc) {
        log.warn("Invalid token: {}", exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(exc.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(AIAssessmentException.class)
    public ResponseEntity<ErrorResponse> handleAIAssessmentException(AIAssessmentException exc) {
        log.error("An error occurred during AI assessment: {}", exc.getMessage(), exc);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(createErrorResponse("An error occurred during AI assessment.", HttpStatus.BAD_GATEWAY));
    }
}
