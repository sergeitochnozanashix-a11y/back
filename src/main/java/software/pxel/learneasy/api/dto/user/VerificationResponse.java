package software.pxel.learneasy.api.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Модель ответа для успешной верификации email")
public record VerificationResponse(
        @Schema(description = "Имя пользователя", example = "testuser")
        String username,

        @Schema(description = "JsonWebToken для доступа к защищенным ресурсам")
        String accessToken,

        @JsonIgnore
        String refreshToken
) {
}
