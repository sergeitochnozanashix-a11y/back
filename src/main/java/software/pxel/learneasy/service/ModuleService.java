package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.module.ModuleDetailsDTO;
import software.pxel.learneasy.api.dto.module.ModuleRequest;
import software.pxel.learneasy.api.dto.module.ModuleResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithLessonList;

import java.time.LocalDateTime;

public interface ModuleService {
    void deleteModuleById(Long id);

    ModuleWithLessonList getModuleById(Long id);

    ModuleResponse createModule(ModuleRequest module);

    ModuleResponse updateModule(Long id, ModuleRequest module);

    Page<ModuleResponse> getAllModules(String title,
                                       String description,
                                       Long courseId,
                                       LocalDateTime createdAfter,
                                       LocalDateTime createdBefore,
                                       Pageable pageable);

    ModuleDetailsDTO getModuleDetailsWithProgress(Long courseId, Long moduleId, Long userId);
}
