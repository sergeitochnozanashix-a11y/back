package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.controller.api.TestApi;
import software.pxel.learneasy.api.dto.test.ExamDTO;
import software.pxel.learneasy.api.dto.test.TestRequest;
import software.pxel.learneasy.api.dto.test.TestResponse;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.TestService;

import static software.pxel.learneasy.constants.ApiRoutes.TESTS_API_URI;

@RestController
@RequestMapping(TESTS_API_URI)
@RequiredArgsConstructor
public class TestController implements TestApi {

    private final TestService testService;

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TestResponse> createTest(@Valid @RequestBody TestRequest testRequest) {
        TestResponse createdTest = testService.createTest(testRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTest);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<TestResponse> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getTestById(id));
    }

    @Override
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<TestResponse> getTestByLessonId(@PathVariable Long lessonId) {
        return ResponseEntity.ok(testService.getTestByLessonId(lessonId));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TestResponse> updateTest(@PathVariable Long id, @Valid @RequestBody TestRequest testRequest) {
        return ResponseEntity.ok(testService.updateTest(id, testRequest));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        testService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api/v1/exams")
    public ResponseEntity<Page<ExamDTO>> getCourseExams(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<ExamDTO> examsPage = testService.getExamsByCourse(courseId, currentUser.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(examsPage);
    }
}
