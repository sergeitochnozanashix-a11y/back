package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.api.dto.lesson.*;
import software.pxel.learneasy.controller.api.LessonApi;
import software.pxel.learneasy.service.LessonService;

import static software.pxel.learneasy.constants.ApiRoutes.LESSON_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(LESSON_URI)
public class LessonController implements LessonApi {

    private final LessonService lessonService;

    @Override
    @GetMapping("/module/{moduleId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<Page<LessonWithoutContentList>> getLessonsByModuleId(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(lessonService.getLessonsByModuleId(
                moduleId,
                pageable
        ));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<LessonWithContentList> getLessonById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LessonWithContentList> createLesson(@Valid @RequestBody LessonRequest lessonRequest) {
        return ResponseEntity.status(201).body(lessonService.createLesson(lessonRequest));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LessonWithContentList> updateLesson(@PathVariable Long id, @Valid @RequestBody UpdateLessonRequest lesson) {
        return ResponseEntity.ok(lessonService.updateLesson(id, lesson));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLessonById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{lessonId}/content")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LessonWithContentList> updateLessonContent(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonContentUpdateRequest request) {
        LessonWithContentList updatedLesson = lessonService.updateLessonContent(lessonId, request.contentBlocks());
        return ResponseEntity.ok(updatedLesson);
    }

    @Override
    @PatchMapping("/{lessonId}/content/markdown")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LessonWithContentList> updateLessonMarkdownContent(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonMarkdownContentUpdateRequest request) {
        LessonWithContentList updatedLesson = lessonService.updateLessonMarkdownContent(lessonId, request.content());
        return ResponseEntity.ok(updatedLesson);
    }

    @Override
    @GetMapping("/{lessonId}/sequence")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<LessonSequenceDTO> getLessonSequenceOrders(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getLessonSequenceOrders(lessonId));
    }
}
