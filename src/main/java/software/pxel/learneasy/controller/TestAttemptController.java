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
import software.pxel.learneasy.controller.api.TestAttemptApi;
import software.pxel.learneasy.api.dto.attempt.CreateAttemptRequest;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.TestAttemptService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test-attempts")
@RequiredArgsConstructor
public class TestAttemptController implements TestAttemptApi {

    private final TestAttemptService service;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public ResponseEntity<TestAttemptResponse> createAttempt(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateAttemptRequest request) {

        CreateAttemptRequest normalized = new CreateAttemptRequest(
                request.testId(),
                request.answers() == null ? List.of() : request.answers()
        );

        TestAttemptResponse created = service.createAttempt(currentUser.getId(), normalized);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @GetMapping("/tests/{testId}/last")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public ResponseEntity<TestAttemptResponse> getLastAttempt(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long testId) {

        return ResponseEntity.ok(service.getLastAttempt(currentUser.getId(), testId));
    }

    @Override
    @GetMapping("/tests/{testId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public ResponseEntity<Page<TestAttemptResponse>> getAttempts(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        return ResponseEntity.ok(service.getAttempts(
                currentUser.getId(),
                testId,
                PageRequest.of(page, size)
        ));
    }
}
