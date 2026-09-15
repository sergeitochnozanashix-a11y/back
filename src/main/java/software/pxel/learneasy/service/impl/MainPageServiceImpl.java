package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.mainpage.CourseInfoDTO;
import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;
import software.pxel.learneasy.api.dto.mainpage.ModuleInfoDTO;
import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.service.MainPageService;
import software.pxel.learneasy.service.UserProgressService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MainPageServiceImpl implements MainPageService {

    private final UserProgressService userProgressService;
    private final CourseRepository courseRepository;
    private final TestAttemptRepository testAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "mainPageInfo", key = "#userId")
    public MainPageInfoResponse getMainPageInfo(Long userId) {
        // Курс определяется по последней активности пользователя. Раньше здесь
        // стоял захардкоженный id=1: главная падала с 404 у всех, как только
        // этот курс удаляли, и показывала чужой курс тем, кто его не проходил.
        Course course = findLastActiveCourse(userId).orElse(null);

        if (course == null) {
            // Новый аккаунт без активности - обычное состояние, а не ошибка.
            return new MainPageInfoResponse(null, List.of(), List.of());
        }

        Long courseId = course.getId();
        CourseProgressResponse progress = userProgressService.getCourseProgress(userId, courseId);
        List<ModuleWithProgressDTO> modulesWithProgress = userProgressService.getModulesWithProgress(courseId, userId);

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
                        // description был доступен в источнике, но терялся при
                        // маппинге - в спеке он при этом значился.
                        m.description(),
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

    /**
     * Курс последней активности. Пусто, если пользователь ещё ничего не проходил
     * или курс успели удалить - оба случая ведут к пустой главной, а не к ошибке.
     */
    private Optional<Course> findLastActiveCourse(Long userId) {
        return testAttemptRepository.findLastActiveCourseIds(userId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .flatMap(courseRepository::findById);
    }
}
