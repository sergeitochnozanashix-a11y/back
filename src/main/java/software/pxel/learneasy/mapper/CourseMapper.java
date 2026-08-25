package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;
import software.pxel.learneasy.model.Course;

@Mapper(componentModel = "spring",
        builder = @Builder(disableBuilder = true), uses = {ModuleMapper.class})
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "modules", ignore = true)
    Course toCourse(CourseRequest courseRequest);

    CourseResponse toCourseResponse(Course course);
}
