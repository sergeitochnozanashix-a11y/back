package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.CourseMapper;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.service.CourseService;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest courseRequest) {
        log.info("Attempting to create new course.");
        Course courseToCreate = courseMapper.toCourse(courseRequest);
        if (courseToCreate == null) {
            log.warn("Course data for creation is null.");
            throw new BadRequestException("Course data cannot be null for creation.");
        }
        if (courseToCreate.getId() != null) {
            log.warn("Attempt to create course with pre-set ID: {}. Not allowed.", courseToCreate.getId());
            throw new BadRequestException("ID must be null for new course creation.");
        }
        courseToCreate.setModules(new ArrayList<>());

        Course savedCourse = courseRepository.save(courseToCreate);
        log.info("Successfully created course ID: {}, title: '{}'", savedCourse.getId(), savedCourse.getTitle());
        return courseMapper.toCourseResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        log.debug("Attempting to find course by ID: {}", id);
        if (id == null) {
            log.warn("Attempt to find course with null ID.");
            throw new BadRequestException("Course ID cannot be null.");
        }
        return courseRepository.findById(id)
                .map(courseMapper::toCourseResponse)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new ResourceNotFoundException("Course not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable)
                .map(courseMapper::toCourseResponse);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest courseRequest) {
        log.info("Attempting to update course with ID: {}", id);

        if (id == null) {
            log.warn("Attempt to update course with null path ID.");
            throw new BadRequestException("Course ID in path for update cannot be null.");
        }
        Course courseUpdates = courseMapper.toCourse(courseRequest);
        if (courseUpdates == null) {
            log.warn("Course data for update is null for ID: {}", id);
            throw new BadRequestException("Course data cannot be null for update.");
        }
        if (courseUpdates.getId() != null && !courseUpdates.getId().equals(id)) {
            log.warn("Mismatched ID during course update. Path ID: {}, Body ID: {}.", id, courseUpdates.getId());
            throw new BadRequestException("Path ID (" + id + ") does not match ID in request body (" + courseUpdates.getId() + ").");
        }

        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Course not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Course not found with id: " + id + " for update.");
                });

        existingCourse.setTitle(courseUpdates.getTitle());
        existingCourse.setDescription(courseUpdates.getDescription());

        Course updatedCourse = courseRepository.save(existingCourse);
        log.info("Successfully updated course ID: {}, new title: '{}'", updatedCourse.getId(), updatedCourse.getTitle());
        return courseMapper.toCourseResponse(updatedCourse);
    }

    @Override
    @Transactional
    public void deleteCourseById(Long id) {
        log.info("Attempting to delete course with ID: {}", id);
        if (id == null) {
            log.warn("Attempt to delete course with null ID.");
            throw new BadRequestException("Course ID for deletion cannot be null.");
        }
        if (!courseRepository.existsById(id)) {
            log.warn("Course not found for deletion with ID: {}", id);
            throw new ResourceNotFoundException("Course not found with id: " + id + ". Cannot delete.");
        }
        courseRepository.deleteById(id);
        log.info("Successfully deleted course with ID: {}", id);
    }
}
