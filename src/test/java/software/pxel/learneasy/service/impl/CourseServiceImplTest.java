package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.CourseMapper;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.repository.CourseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course course;
    private CourseResponse courseResponse;
    private CourseRequest courseRequest;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Test Course");
        course.setDescription("Description for Test Course");
        course.setModules(new ArrayList<>());

        courseResponse = new CourseResponse(
                1L,
                "Test Course",
                "Description for Test Course"
        );

        courseRequest = new CourseRequest(
                "New Course",
                "New Description"
        );
    }

    @Test
    @DisplayName("createCourse - Success")
    void createCourse_success() {
        Course savedCourse = new Course();
        savedCourse.setTitle(courseRequest.title());
        savedCourse.setDescription(courseRequest.description());

        when(courseMapper.toCourse(any(CourseRequest.class))).thenReturn(savedCourse);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        when(courseMapper.toCourseResponse(any(Course.class))).thenReturn(courseResponse);

        CourseResponse result = courseService.createCourse(courseRequest);

        assertNotNull(result);
        assertEquals(courseResponse.id(), result.id());
        assertEquals(courseResponse.title(), result.title());
        assertEquals(courseResponse.description(), result.description());

        verify(courseMapper, times(1)).toCourse(courseRequest);
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(courseMapper, times(1)).toCourseResponse(savedCourse);
    }

    @Test
    @DisplayName("createCourse - Throws BadRequestException when request body is null")
    void createCourse_throwsBadRequestException_whenRequestBodyIsNull() {
        when(courseMapper.toCourse(eq(null))).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.createCourse(null));

        assertEquals("Course data cannot be null for creation.", exception.getMessage());

        verify(courseMapper, times(1)).toCourse(eq(null));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("createCourse - Throws BadRequestException when ID is not null")
    void createCourse_throwsBadRequestException_whenIdIsNotNull() {
        Course courseWithId = new Course();
        courseWithId.setId(5L);

        CourseRequest requestWithId = new CourseRequest(
                "Title",
                "Description"
        );

        when(courseMapper.toCourse(any(CourseRequest.class))).thenReturn(courseWithId);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.createCourse(requestWithId));

        assertEquals("ID must be null for new course creation.", exception.getMessage());

        verify(courseMapper, times(1)).toCourse(requestWithId);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("getCourseById - Success")
    void getCourseById_success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toCourseResponse(course)).thenReturn(courseResponse);

        CourseResponse result = courseService.getCourseById(1L);

        assertNotNull(result);
        assertEquals(courseResponse, result);
        verify(courseRepository, times(1)).findById(1L);
        verify(courseMapper, times(1)).toCourseResponse(course);
    }

    @Test
    @DisplayName("getCourseById - Throws ResourceNotFoundException when course not found")
    void getCourseById_throwsResourceNotFoundException_whenCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCourseById(99L));

        assertEquals("Course not found with id: 99", exception.getMessage());
        verify(courseRepository, times(1)).findById(99L);
        verify(courseMapper, never()).toCourseResponse(any(Course.class));
    }

    @Test
    @DisplayName("getCourseById - Throws BadRequestException when ID is null")
    void getCourseById_throwsBadRequestException_whenIdIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.getCourseById(null));

        assertEquals("Course ID cannot be null.", exception.getMessage());
        verify(courseRepository, never()).findById(anyLong());
        verify(courseMapper, never()).toCourseResponse(any(Course.class));
    }

    @Test
    @DisplayName("getAllCourses - Success with no filters")
    void getAllCourses_successWithNoFilters() {
        List<Course> courseList = List.of(course);
        Page<Course> coursePage = new PageImpl<>(courseList);
        Pageable pageable = Pageable.unpaged();

        when(courseRepository.findAll(eq(pageable))).thenReturn(coursePage);
        when(courseMapper.toCourseResponse(any(Course.class))).thenReturn(courseResponse);

        Page<CourseResponse> result = courseService.getAllCourses(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(courseResponse, result.getContent().getFirst());
        verify(courseRepository, times(1)).findAll(eq(pageable));
        verify(courseMapper, times(1)).toCourseResponse(any(Course.class));
    }

    @Test
    @DisplayName("updateCourse - Success")
    void updateCourse_success() {
        Course updatedCourse = new Course();
        updatedCourse.setId(1L);
        updatedCourse.setTitle("Updated Title");
        updatedCourse.setDescription("Updated Description");

        CourseRequest updateRequest = new CourseRequest(
                "Updated Title",
                "Updated Description"
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toCourse(any(CourseRequest.class))).thenReturn(updatedCourse);
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
        when(courseMapper.toCourseResponse(updatedCourse)).thenReturn(new CourseResponse(1L, "Updated Title", "Updated Description"));

        CourseResponse result = courseService.updateCourse(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Title", result.title());
        assertEquals("Updated Description", result.description());
        verify(courseRepository, times(1)).findById(1L);
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(courseMapper, times(1)).toCourse(updateRequest);
        verify(courseMapper, times(1)).toCourseResponse(updatedCourse);
    }

    @Test
    @DisplayName("updateCourse - Throws BadRequestException when path ID is null")
    void updateCourse_throwsBadRequestException_whenPathIdIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.updateCourse(null, courseRequest));

        assertEquals("Course ID in path for update cannot be null.", exception.getMessage());
        verify(courseRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateCourse - Throws BadRequestException when request body is null")
    void updateCourse_throwsBadRequestException_whenRequestBodyIsNull() {
        when(courseMapper.toCourse(eq(null))).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.updateCourse(1L, null));

        assertEquals("Course data cannot be null for update.", exception.getMessage());

        verify(courseMapper, times(1)).toCourse(eq(null));
        verify(courseRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateCourse - Throws ResourceNotFoundException when course not found for update")
    void updateCourse_throwsResourceNotFoundException_whenCourseNotFoundForUpdate() {
        Long nonExistentCourseId = 99L;
        CourseRequest updateRequest = new CourseRequest(
                "Updated Title",
                "Updated Description"
        );
        Course courseUpdates = new Course();

        when(courseMapper.toCourse(any(CourseRequest.class))).thenReturn(courseUpdates);
        when(courseRepository.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> courseService.updateCourse(nonExistentCourseId, updateRequest));

        assertEquals("Course not found with id: 99 for update.", exception.getMessage());
        verify(courseRepository, times(1)).findById(nonExistentCourseId);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("deleteCourseById - Success")
    void deleteCourseById_success() {
        Long courseId = 1L;
        when(courseRepository.existsById(courseId)).thenReturn(true);
        doNothing().when(courseRepository).deleteById(anyLong());

        courseService.deleteCourseById(courseId);

        verify(courseRepository, times(1)).existsById(courseId);
        verify(courseRepository, times(1)).deleteById(courseId);
    }

    @Test
    @DisplayName("deleteCourseById - Throws BadRequestException when ID is null")
    void deleteCourseById_throwsBadRequestException_whenIdIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> courseService.deleteCourseById(null));

        assertEquals("Course ID for deletion cannot be null.", exception.getMessage());
        verify(courseRepository, never()).existsById(anyLong());
        verify(courseRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteCourseById - Throws ResourceNotFoundException when course not found")
    void deleteCourseById_throwsResourceNotFoundException_whenCourseNotFound() {
        Long nonExistentCourseId = 99L;
        when(courseRepository.existsById(nonExistentCourseId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> courseService.deleteCourseById(nonExistentCourseId));

        assertEquals("Course not found with id: 99. Cannot delete.", exception.getMessage());
        verify(courseRepository, times(1)).existsById(nonExistentCourseId);
        verify(courseRepository, never()).deleteById(anyLong());
    }
}
