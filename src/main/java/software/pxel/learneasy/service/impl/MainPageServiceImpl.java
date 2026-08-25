package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.mainpage.CourseInfoDTO;
import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;
import software.pxel.learneasy.api.dto.mainpage.ModuleInfoDTO;
import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.service.MainPageService;
import software.pxel.learneasy.service.UserProgressService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MainPageServiceImpl implements MainPageService {

    private static final long DEFAULT_COURSE_ID = 1L;

    private final UserProgressService userProgressService;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "mainPageInfo", key = "#userId")
    public MainPageInfoResponse getMainPageInfo(Long userId) {
        Course course = courseRepository.findById(DEFAULT_COURSE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Default course with ID " + DEFAULT_COURSE_ID + " not found."));

        CourseProgressResponse progress = userProgressService.getCourseProgress(userId, DEFAULT_COURSE_ID);
        List<ModuleWithProgressDTO> modulesWithProgress = userProgressService.getModulesWithProgress(DEFAULT_COURSE_ID, userId);

        Long currentModuleId = modulesWithProgress.stream()
                .filter(module -> !Objects.equals(module.completionStatus(), CompletionStatus.COMPLETED.toString()))
                .map(ModuleWithProgressDTO::id)
                .findFirst()
                .orElse(null);

        CourseInfoDTO courseInfo = new CourseInfoDTO(
                course.getId(),
                course.getTitle(),
                currentModuleId,
                progress.totalModules(),
                progress.totalLessons(),
                progress.completedModules(),
                progress.completedLessons()
        );

        List<ModuleInfoDTO> modulesInfo = modulesWithProgress.stream()
                .map(m -> new ModuleInfoDTO(
                        m.id(),
                        m.sequenceOrder(),
                        m.title(),
                        m.totalLessons(),
                        m.completedLessons(),
                        m.completionStatus()
                ))
                .toList();

        // Последние 7 дней включая сегодня
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<UserActivityStatsResponse> weeklyActivity =
                userProgressService.getUserActivityStats(userId, start, end);

        return new MainPageInfoResponse(courseInfo, modulesInfo, weeklyActivity);
    }
}
