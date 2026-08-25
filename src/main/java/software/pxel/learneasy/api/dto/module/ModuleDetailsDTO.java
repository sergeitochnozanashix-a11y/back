package software.pxel.learneasy.api.dto.module;

import software.pxel.learneasy.api.dto.userprogress.LessonProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.ModuleInfoDTO;

import java.util.List;

public record ModuleDetailsDTO(
        ModuleInfoDTO moduleInfo,
        List<LessonProgressDTO> lessons
) {
}
