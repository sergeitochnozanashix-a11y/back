package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;

import java.time.LocalDate;
import java.util.List;

public interface UserProgressService {

    CourseProgressResponse getCourseProgress(Long userId, Long courseId);

    List<ModuleWithProgressDTO> getModulesWithProgress(Long courseId, Long userId);

    /**
     * Ежедневная активность пользователя за период [start, end] включительно.
     * Возвращает список по дням (нулевые дни присутствуют).
     */
    List<UserActivityStatsResponse> getUserActivityStats(Long userId, LocalDate start, LocalDate end);
}
