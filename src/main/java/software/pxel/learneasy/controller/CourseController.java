package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.controller.api.CourseApi;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.CourseService;
import software.pxel.learneasy.service.UserProgressService;

import java.util.List;

import static software.pxel.learneasy.constants.ApiRoutes.COURSES_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(COURSES_URI)
public class CourseController implements CourseApi {

    private final CourseService courseService;
    private final UserProgressService userProgressService;

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest course) {
        return ResponseEntity.status(201).body(courseService.createCourse(course));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest course) {
        return ResponseEntity.ok(courseService.updateCourse(id, course));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourseById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<Page<CourseResponse>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(courseService.getAllCourses(pageable));
    }

    @Override
    @GetMapping("/{courseId}/modules")
    public ResponseEntity<List<ModuleWithProgressDTO>> getModulesWithProgress(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = currentUser.getId();
        return ResponseEntity.ok(userProgressService.getModulesWithProgress(courseId, userId));
    }
}
