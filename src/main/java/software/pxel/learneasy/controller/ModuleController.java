package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.controller.api.ModuleApi;
import software.pxel.learneasy.api.dto.module.ModuleDetailsDTO;
import software.pxel.learneasy.api.dto.module.ModuleRequest;
import software.pxel.learneasy.api.dto.module.ModuleResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithLessonList;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.ModuleService;

import java.time.LocalDateTime;

import static software.pxel.learneasy.constants.ApiRoutes.MODULES_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(MODULES_URI)
public class ModuleController implements ModuleApi {

    private final ModuleService moduleService;

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<ModuleWithLessonList> getModuleById(@PathVariable Long id) {
        return ResponseEntity.ok(moduleService.getModuleById(id));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ModuleResponse> createModule(@Valid @RequestBody ModuleRequest module) {
        return ResponseEntity.status(201).body(moduleService.createModule(module));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ModuleResponse> updateModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest module) {
        return ResponseEntity.ok(moduleService.updateModule(id, module));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        moduleService.deleteModuleById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<Page<ModuleResponse>> getAllModules(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {

        String[] sortParams;
        try {
            if (sort == null || !sort.contains(",")) {
                throw new IllegalArgumentException("Malformed sort parameter");
            }
            sortParams = sort.split(",");
            if (sortParams.length != 2 || sortParams[0].isEmpty() || sortParams[1].isEmpty()) {
                throw new IllegalArgumentException("Malformed sort parameter");
            }
        } catch (IllegalArgumentException e) {
            sortParams = new String[]{"title", "asc"};
        }
        Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, direction, sortParams[0]);

        return ResponseEntity.ok(moduleService.getAllModules(
                title,
                description,
                courseId,
                createdAfter,
                createdBefore,
                pageable
        ));
    }

    @Override
    @GetMapping("/{courseId}/progress/{moduleId}")
    public ModuleDetailsDTO getModuleDetailsWithProgress(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        return moduleService.getModuleDetailsWithProgress(courseId, moduleId, userId);
    }
}
