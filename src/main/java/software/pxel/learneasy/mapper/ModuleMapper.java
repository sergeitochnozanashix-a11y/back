package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import software.pxel.learneasy.api.dto.module.ModuleRequest;
import software.pxel.learneasy.api.dto.module.ModuleResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithLessonList;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.Module;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = {LessonMapper.class})
public interface ModuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "course", source = "courseId", qualifiedByName = "idToCourse")
    Module toModule(ModuleRequest moduleRequest);

    @Named("idToCourse")
    default Course idToCourse(Long id) {
        if (id == null) return null;
        Course c = new Course();
        c.setId(id);
        return c;
    }

    @Mapping(target = "lessons", source = "lessons", qualifiedByName = "mapLessonsToListLessonWithoutContent")
    ModuleWithLessonList toModuleWithLessonListDTO(Module module);

    @Mapping(target = "courseId", expression = "java(module.getCourse() != null ? module.getCourse().getId() : null)")
    ModuleResponse toModuleResponse(Module module);
}
