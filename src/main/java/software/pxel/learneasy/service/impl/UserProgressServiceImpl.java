package software.pxel.learneasy.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.api.dto.userprogress.ModuleProgressDTO;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.*;
import software.pxel.learneasy.repository.projection.ModuleWithLessonCountsProjection;
import software.pxel.learneasy.service.UserProgressService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserProgressServiceImpl implements UserProgressService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final TestModelRepository testModelRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courseProgress", key = "#userId + '::' + #courseId")
    public CourseProgressResponse getCourseProgress(Long userId, Long courseId) {
        userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        List<Module> allModulesInCourse = moduleRepository.findAllByCourseId(courseId);
        long totalModules = allModulesInCourse.size();
        long totalLessons = allModulesInCourse.stream().mapToLong(module -> module.getLessons().size()).sum();

        long completedLessons = testAttemptRepository.countDistinctPassedLessonsByUserIdAndCourseId(userId, courseId);

        Map<Long, Set<Long>> lessonsWithTestsByModule = testModelRepository.findModuleLessonPairsWithTestsByCourseId(courseId)
                .stream()
                .collect(Collectors.groupingBy(
                        TestModelRepository.ModuleLessonPair::getModuleId,
                        Collectors.mapping(TestModelRepository.ModuleLessonPair::getLessonId, Collectors.toSet())
                ));

        Set<Long> passedLessonIds = testAttemptRepository.findPassedLessonIdsByUserIdAndCourseId(userId, courseId);

        long completedModules = 0;
        List<ModuleProgressDTO> moduleProgressList = new ArrayList<>();

        for (Module module : allModulesInCourse) {
            Set<Long> lessonsWithTests = lessonsWithTestsByModule.getOrDefault(module.getId(), Collections.emptySet());
            boolean isCompleted = lessonsWithTests.isEmpty() || passedLessonIds.containsAll(lessonsWithTests);

            if (isCompleted) {
                completedModules++;
            }
            moduleProgressList.add(new ModuleProgressDTO(module.getTitle(), isCompleted));
        }

        return new CourseProgressResponse(
                totalModules,
                completedModules,
                totalLessons,
                completedLessons,
                moduleProgressList
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "modulesProgress", key = "#userId + '::' + #courseId")
    public List<ModuleWithProgressDTO> getModulesWithProgress(Long courseId, Long userId) {
        List<ModuleWithLessonCountsProjection> moduleLessonCounts =
                moduleRepository.findModulesWithLessonCountByCourseId(courseId);

        Map<Long, Set<Long>> lessonsWithTestsByModule = testModelRepository.findModuleLessonPairsWithTestsByCourseId(courseId)
                .stream()
                .collect(Collectors.groupingBy(
                        TestModelRepository.ModuleLessonPair::getModuleId,
                        Collectors.mapping(TestModelRepository.ModuleLessonPair::getLessonId, Collectors.toSet())
                ));

        Set<Long> passedTestLessonIds = testAttemptRepository.findPassedLessonIdsByUserIdAndCourseId(userId, courseId);

        Map<Long, Long> completedLessonsByModuleId = passedTestLessonIds.stream()
                .map(lessonId -> testModelRepository.findByLessonId(lessonId)
                        .map(testModel -> testModel.getModule().getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        moduleId -> moduleId,
                        Collectors.counting()
                ));

        AtomicBoolean isPreviousModuleCompleted = new AtomicBoolean(true);

        return moduleLessonCounts.stream()
                .map(module -> {
                    Long moduleId = module.getModuleId();
                    long completedLessons = completedLessonsByModuleId.getOrDefault(moduleId, 0L);
                    int testableLessons = lessonsWithTestsByModule.getOrDefault(moduleId, Set.of()).size();
                    boolean isModuleCompleted = (testableLessons > 0 && completedLessons >= testableLessons) || testableLessons == 0;

                    String completionStatus;
                    if (!isPreviousModuleCompleted.get()) {
                        completionStatus = CompletionStatus.BLOCKED.toString();
                    } else if (isModuleCompleted) {
                        completionStatus = CompletionStatus.COMPLETED.toString();
                    } else if (completedLessons > 0) {
                        completionStatus = CompletionStatus.IN_PROGRESS.toString();
                    } else {
                        completionStatus = CompletionStatus.NOT_STARTED.toString();
                    }

                    if (!completionStatus.equals(CompletionStatus.BLOCKED.toString())) {
                        isPreviousModuleCompleted.set(completionStatus.equals(CompletionStatus.COMPLETED.toString()));
                    }

                    return new ModuleWithProgressDTO(
                            moduleId,
                            module.getSequenceOrder(),
                            module.getTitle(),
                            module.getDescription(),
                            module.getLessonCount().intValue(),
                            completedLessons,
                            completionStatus
                    );
                })
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<UserActivityStatsResponse> getUserActivityStats(Long userId, LocalDate start, LocalDate end) {
        ZoneId utcZone = ZoneOffset.UTC;
        Instant startI = start.atStartOfDay(utcZone).toInstant();
        Instant endI = end.plusDays(1).atStartOfDay(utcZone).minusNanos(1).toInstant();

        Map<LocalDate, Long> testsByDay = new HashMap<>();
        testAttemptRepository.findDailyTestStats(userId, startI, endI).forEach(row -> {
            LocalDate day = toLocalDate(row[0]);
            Long cnt = toLong(row[1]);
            testsByDay.put(day, cnt);
        });

        List<UserActivityStatsResponse> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long tests = testsByDay.getOrDefault(d, 0L);
            result.add(new UserActivityStatsResponse(d, 0L, tests));
        }
        return result;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (o instanceof LocalDate ld) return ld;
        throw new IllegalStateException("Unexpected date type: " + (o == null ? "null" : o.getClass()));
    }

    private static Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        throw new IllegalStateException("Unexpected number type: " + (o == null ? "null" : o.getClass()));
    }
}
