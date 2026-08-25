package software.pxel.learneasy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.controller.api.UserProgressApi;
import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.MainPageService;
import software.pxel.learneasy.service.UserProgressService;
import software.pxel.learneasy.service.UserService;

import static software.pxel.learneasy.constants.ApiRoutes.USER_PROGRESS_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(USER_PROGRESS_URI)
public class UserProgressController implements UserProgressApi {

    private final UserProgressService userProgressService;
    private final MainPageService mainPageService;
    private final UserService userService;

    @Override
    @GetMapping("/users/{userId}/courses/{courseId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(userProgressService.getCourseProgress(userId, courseId));
    }

    @Override
    @GetMapping("/users/main-page")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public ResponseEntity<MainPageInfoResponse> getMainPageInfo(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal User currentUser) {

        boolean isAdmin = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ADMIN"::equals);

        if (userId != null && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to query another user's main page.");
        }

        Long effectiveUserId;
        if (userId != null) {
            userService.findUserById(userId);
            effectiveUserId = userId;
        } else {
            effectiveUserId = currentUser.getId();
        }

        return ResponseEntity.ok(mainPageService.getMainPageInfo(effectiveUserId));
    }
}
