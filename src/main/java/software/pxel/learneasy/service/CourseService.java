package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;

public interface CourseService {

    CourseResponse createCourse(CourseRequest courseRequest);

    CourseResponse getCourseById(Long id);

    Page<CourseResponse> getAllCourses(Pageable pageable);

    CourseResponse updateCourse(Long id, CourseRequest courseRequest);

    void deleteCourseById(Long id);
}
