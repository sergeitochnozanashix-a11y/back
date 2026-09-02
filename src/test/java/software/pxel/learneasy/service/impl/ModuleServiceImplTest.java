package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import software.pxel.learneasy.api.dto.module.ModuleDetailsDTO;
import software.pxel.learneasy.api.dto.module.ModuleRequest;
import software.pxel.learneasy.api.dto.module.ModuleResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithLessonList;
import software.pxel.learneasy.api.dto.userprogress.LessonProgressDTO;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.ModuleMapper;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.repository.ModuleRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.TestModelRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService — операции с модулями")
class ModuleServiceImplTest {

    @Mock
    ModuleRepository moduleRepository;
    @Mock
    CourseRepository courseRepository;
    @Mock
    TestModelRepository testModelRepository;
    @Mock
    TestAttemptRepository testAttemptRepository;
    @Mock
    ModuleMapper moduleMapper;

    @InjectMocks
    ModuleServiceImpl service;

    // ------------------------------------------------------------
    @Nested
    @DisplayName("createModule(request) — создание")
    class CreateModule {

        @Test
        @DisplayName("ошибка — курс не найден")
        void error_courseNotFound() {
            ModuleRequest req = new ModuleRequest("T", "D", 10L, 1);
            when(courseRepository.findById(10L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.createModule(req));
            assertTrue(ex.getMessage().contains("Course not found with id: 10"));

            verify(courseRepository).findById(10L);
            verifyNoInteractions(moduleMapper, moduleRepository);
        }

        @Test
        @DisplayName("ошибка — mapper вернул null")
        void error_mapperReturnsNull() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 1);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            when(moduleMapper.toModule(req)).thenReturn(null);

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createModule(req));
            assertTrue(ex.getMessage().contains("cannot be null"));

            verify(courseRepository).findById(1L);
            verify(moduleMapper).toModule(req);
            verifyNoInteractions(moduleRepository);
        }

        @Test
        @DisplayName("ошибка — у создаваемого модуля уже задан ID")
        void error_idPreset() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 1);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            Module m = new Module();
            m.setId(99L);
            when(moduleMapper.toModule(req)).thenReturn(m);

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createModule(req));
            assertTrue(ex.getMessage().contains("ID must be null"));

            verify(moduleMapper).toModule(req);
            verifyNoInteractions(moduleRepository);
        }

        @Test
        @DisplayName("ошибка — конфликт sequenceOrder (уже существует)")
        void error_sequenceConflict() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 3);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            when(moduleMapper.toModule(req)).thenReturn(new Module());
            when(moduleRepository.findAllSequenceOrderByCourseId(1L)).thenReturn(List.of(1, 3, 5));

            ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.createModule(req));
            assertTrue(ex.getMessage().contains("Sequence order cannot be duplicated"));
            assertTrue(ex.getMessage().contains("Max sequence order: 5"));
            verify(moduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("успех — пустые sequence в курсе → sequenceOrder=0, курс и уроки проставлены, сохранён и замаплен")
        void success_emptySequenceList_setsZero() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 7);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            Module newM = new Module();
            when(moduleMapper.toModule(req)).thenReturn(newM);
            when(moduleRepository.findAllSequenceOrderByCourseId(1L)).thenReturn(Collections.emptyList());

            Module saved = new Module();
            saved.setId(10L);
            saved.setTitle("T");
            when(moduleRepository.save(any(Module.class))).thenReturn(saved);
            ModuleResponse resp = new ModuleResponse(10L, "T", "D", 1L, 0);
            when(moduleMapper.toModuleResponse(saved)).thenReturn(resp);

            ModuleResponse out = service.createModule(req);

            assertEquals(10L, out.id());
            assertEquals(0, out.sequenceOrder());
            ArgumentCaptor<Module> cap = ArgumentCaptor.forClass(Module.class);
            verify(moduleRepository).save(cap.capture());
            Module toSave = cap.getValue();
            assertEquals(0, toSave.getSequenceOrder());
            assertNotNull(toSave.getCourse());
            assertNotNull(toSave.getLessons());
            assertTrue(toSave.getLessons().isEmpty());
        }

        @Test
        @DisplayName("успех — sequenceList без конфликта → sequenceOrder как в запросе")
        void success_sequenceOk_keepsRequestedOrder() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 4);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            when(moduleMapper.toModule(req)).thenReturn(new Module());
            when(moduleRepository.findAllSequenceOrderByCourseId(1L)).thenReturn(List.of(1, 2, 3));

            Module saved = new Module();
            saved.setId(20L);
            saved.setSequenceOrder(4);
            when(moduleRepository.save(any(Module.class))).thenReturn(saved);
            when(moduleMapper.toModuleResponse(saved)).thenReturn(new ModuleResponse(20L, "T", "D", 1L, 4));

            ModuleResponse out = service.createModule(req);
            assertEquals(4, out.sequenceOrder());
        }
    }

    @Nested
    @DisplayName("getModuleById(id) — чтение с уроками")
    class GetModuleById {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.getModuleById(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — модуль не найден")
        void error_notFound() {
            when(moduleRepository.findByIdWithLessons(5L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getModuleById(5L));
            assertTrue(ex.getMessage().contains("Module not found with id: 5"));
        }

        @Test
        @DisplayName("успех — маппит в DTO")
        void success_maps() {
            Module m = new Module();
            when(moduleRepository.findByIdWithLessons(7L)).thenReturn(Optional.of(m));
            ModuleWithLessonList dto = ModuleWithLessonList.builder().id(7L).title("T").description("D").lessons(List.of()).build();
            when(moduleMapper.toModuleWithLessonListDTO(m)).thenReturn(dto);

            ModuleWithLessonList out = service.getModuleById(7L);
            assertEquals(7L, out.id());
            verify(moduleMapper).toModuleWithLessonListDTO(m);
        }
    }

    @Nested
    @DisplayName("getAllModules(...) — фильтр+пагинация")
    class GetAllModules {

        @Test
        @DisplayName("успех — спека и маппинг в ответы")
        void success_specAndMapping() {
            Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
            Module m1 = new Module();
            m1.setId(1L);
            Module m2 = new Module();
            m2.setId(2L);
            when(moduleRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(m1, m2), pageable, 2));

            when(moduleMapper.toModuleResponse(m1)).thenReturn(new ModuleResponse(1L, "A", "D1", 1L, 1));
            when(moduleMapper.toModuleResponse(m2)).thenReturn(new ModuleResponse(2L, "B", "D2", 1L, 2));

            Page<ModuleResponse> page = service.getAllModules("A", "desc", 1L,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable);

            assertEquals(2, page.getContent().size());
            assertEquals(2, page.getTotalElements());
            verify(moduleRepository).findAll(any(Specification.class), eq(pageable));
            verify(moduleMapper).toModuleResponse(m1);
            verify(moduleMapper).toModuleResponse(m2);
        }
    }

    @Nested
    @DisplayName("updateModule(id, request) — обновление")
    class UpdateModule {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 1);
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.updateModule(null, req));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — request == null")
        void error_nullRequest() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.updateModule(1L, null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — курс не найден")
        void error_courseNotFound() {
            ModuleRequest req = new ModuleRequest("T", "D", 9L, 2);
            when(courseRepository.findById(9L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateModule(1L, req));
            assertTrue(ex.getMessage().contains("Course not found with id: 9"));
        }

        @Test
        @DisplayName("ошибка — модуль не найден")
        void error_moduleNotFound() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 2);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            when(moduleRepository.findById(5L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateModule(5L, req));
            assertTrue(ex.getMessage().contains("Module not found with id: 5 for update."));
        }

        @Test
        @DisplayName("ошибка — конфликт sequenceOrder")
        void error_sequenceConflict() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 3);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            Module existing = new Module();
            existing.setId(7L);
            when(moduleRepository.findById(7L)).thenReturn(Optional.of(existing));
            when(moduleRepository.findAllSequenceOrderByCourseIdExcluding(1L, 7L)).thenReturn(List.of(1, 3, 4));

            assertThrows(ResourceConflictException.class, () -> service.updateModule(7L, req));
            verify(moduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("успех — других модулей в курсе нет → берётся запрошенный sequenceOrder")
        void success_noOtherModules_keepsRequestedOrder() {
            ModuleRequest req = new ModuleRequest("T2", "D2", 1L, 9);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            Module existing = new Module();
            existing.setId(8L);
            when(moduleRepository.findById(8L)).thenReturn(Optional.of(existing));
            when(moduleRepository.findAllSequenceOrderByCourseIdExcluding(1L, 8L)).thenReturn(Collections.emptyList());

            Module saved = new Module();
            saved.setId(8L);
            saved.setSequenceOrder(9);
            when(moduleRepository.save(existing)).thenReturn(saved);
            when(moduleMapper.toModuleResponse(saved)).thenReturn(new ModuleResponse(8L, "T2", "D2", 1L, 9));

            ModuleResponse out = service.updateModule(8L, req);

            assertEquals(9, out.sequenceOrder());
            assertEquals(9, existing.getSequenceOrder());
            assertEquals("T2", out.title());
            assertEquals("D2", out.description());
        }

        @Test
        @DisplayName("успех — нормальная замена полей и sequenceOrder без конфликта")
        void success_updatesFields() {
            ModuleRequest req = new ModuleRequest("NewT", "NewD", 1L, 5);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));
            Module existing = new Module();
            existing.setId(9L);
            when(moduleRepository.findById(9L)).thenReturn(Optional.of(existing));
            when(moduleRepository.findAllSequenceOrderByCourseIdExcluding(1L, 9L)).thenReturn(List.of(1, 2, 3));

            Module saved = new Module();
            saved.setId(9L);
            saved.setTitle("NewT");
            saved.setDescription("NewD");
            saved.setSequenceOrder(5);
            when(moduleRepository.save(existing)).thenReturn(saved);
            when(moduleMapper.toModuleResponse(saved)).thenReturn(new ModuleResponse(9L, "NewT", "NewD", 1L, 5));

            ModuleResponse out = service.updateModule(9L, req);

            assertEquals(9L, out.id());
            assertEquals(5, out.sequenceOrder());
            assertEquals("NewT", out.title());
            assertEquals("NewD", out.description());
        }

        @Test
        @DisplayName("успех — сохранение с собственным неизменным sequenceOrder не конфликтует само с собой")
        void success_keepsOwnSequenceOrder() {
            // Модуль 10 уже стоит на позиции 2. Пользователь правит только
            // заголовок и присылает тот же sequenceOrder - это не дубликат.
            ModuleRequest req = new ModuleRequest("Renamed", "D", 1L, 2);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));

            Module existing = new Module();
            existing.setId(10L);
            existing.setSequenceOrder(2);
            when(moduleRepository.findById(10L)).thenReturn(Optional.of(existing));
            // Позиция 2 принадлежит самому модулю 10, поэтому в выборке её нет.
            when(moduleRepository.findAllSequenceOrderByCourseIdExcluding(1L, 10L)).thenReturn(List.of(1, 3));

            Module saved = new Module();
            saved.setId(10L);
            saved.setTitle("Renamed");
            saved.setSequenceOrder(2);
            when(moduleRepository.save(existing)).thenReturn(saved);
            when(moduleMapper.toModuleResponse(saved)).thenReturn(new ModuleResponse(10L, "Renamed", "D", 1L, 2));

            ModuleResponse out = service.updateModule(10L, req);

            assertEquals(2, out.sequenceOrder());
            assertEquals("Renamed", out.title());
            verify(moduleRepository).save(existing);
        }

        @Test
        @DisplayName("ошибка — занятый чужим модулем sequenceOrder по-прежнему конфликтует")
        void error_sequenceTakenByAnotherModule() {
            ModuleRequest req = new ModuleRequest("T", "D", 1L, 3);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "C")));

            Module existing = new Module();
            existing.setId(10L);
            existing.setSequenceOrder(2);
            when(moduleRepository.findById(10L)).thenReturn(Optional.of(existing));
            // 3 занята другим модулем курса - это настоящий дубликат.
            when(moduleRepository.findAllSequenceOrderByCourseIdExcluding(1L, 10L)).thenReturn(List.of(1, 3));

            ResourceConflictException ex =
                    assertThrows(ResourceConflictException.class, () -> service.updateModule(10L, req));
            assertTrue(ex.getMessage().contains("Sequence order cannot be duplicated"));
            verify(moduleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteModuleById(id) — удаление")
    class DeleteModule {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deleteModuleById(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — модуль не найден")
        void error_notFound() {
            when(moduleRepository.findById(77L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteModuleById(77L));
            assertTrue(ex.getMessage().contains("Module not found with id: 77"));
            verify(moduleRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("успех — удалён по id")
        void success_deleted() {
            when(moduleRepository.findById(3L)).thenReturn(Optional.of(new Module()));

            service.deleteModuleById(3L);

            verify(moduleRepository).deleteById(3L);
        }
    }

    @Nested
    @DisplayName("getModuleDetailsWithProgress(courseId, moduleId, userId) — детали и прогресс")
    class GetModuleDetailsWithProgress {
        private final Long courseId = 1L;
        private final Long moduleId = 10L;
        private final Long userId = 100L;

        @Test
        @DisplayName("ошибка — модуль не найден")
        void error_moduleNotFound() {
            when(moduleRepository.findByIdWithLessons(moduleId)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.getModuleDetailsWithProgress(courseId, moduleId, userId));
            assertTrue(ex.getMessage().contains("Module not found with id: " + moduleId));
        }

        @Test
        @DisplayName("ошибка — модуль не принадлежит курсу")
        void error_moduleNotInCourse() {
            Course course1 = course(courseId, "C1");
            Course course2 = course(2L, "C2");
            Module module = module(moduleId, "M1", course2);

            when(moduleRepository.findByIdWithLessons(moduleId)).thenReturn(Optional.of(module));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.getModuleDetailsWithProgress(courseId, moduleId, userId));
            assertTrue(ex.getMessage().contains("does not belong to course"));
        }

        @Test
        @DisplayName("успех — все уроки с тестами пройдены -> COMPLETED")
        void success_allTestableLessonsPassed_isCompleted() {
            Course course = course(courseId, "C1");
            Module module = module(moduleId, "M1", course);
            Lesson l1 = lesson(101L, module);
            Lesson l2 = lesson(102L, module);

            when(moduleRepository.findByIdWithLessons(moduleId)).thenReturn(Optional.of(module));
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(Set.of(moduleId))).thenReturn(Set.of(101L));
            when(testAttemptRepository.findPassedLessonIdsByUserIdAndModuleIds(userId, Set.of(moduleId))).thenReturn(Set.of(101L));
            TestAttempt attempt = new TestAttempt();
            attempt.setScorePercent(95);
            when(testAttemptRepository.findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(userId, 101L))
                    .thenReturn(Optional.of(attempt));

            ModuleDetailsDTO dto = service.getModuleDetailsWithProgress(courseId, moduleId, userId);

            assertEquals(CompletionStatus.COMPLETED.toString(), dto.moduleInfo().completionStatus());
            assertEquals(2, dto.moduleInfo().completedLessons());
            assertEquals(2, dto.moduleInfo().totalLessons());

            List<LessonProgressDTO> lessons = dto.lessons();
            assertEquals(2, lessons.size());
            assertEquals(CompletionStatus.COMPLETED.toString(), lessons.get(0).status());
            assertEquals(95, lessons.get(0).progress().testResult());
            assertEquals(CompletionStatus.COMPLETED.toString(), lessons.get(1).status());
        }

        @Test
        @DisplayName("успех — есть непройденные уроки с тестами -> IN_PROGRESS / BLOCKED")
        void success_inProgressAndBlocked() {
            Course course = course(courseId, "C1");
            Module module = module(moduleId, "M1", course);
            lesson(101L, module);
            lesson(102L, module);
            lesson(103L, module);

            when(moduleRepository.findByIdWithLessons(moduleId)).thenReturn(Optional.of(module));
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(Set.of(moduleId))).thenReturn(Set.of(101L, 102L));
            when(testAttemptRepository.findPassedLessonIdsByUserIdAndModuleIds(userId, Set.of(moduleId))).thenReturn(Set.of(101L));
            TestAttempt attempt1 = new TestAttempt();
            attempt1.setScorePercent(90);
            TestAttempt attempt2 = new TestAttempt();
            attempt2.setScorePercent(50);
            when(testAttemptRepository.findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(userId, 101L)).thenReturn(Optional.of(attempt1));
            when(testAttemptRepository.findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(userId, 102L)).thenReturn(Optional.of(attempt2));

            ModuleDetailsDTO dto = service.getModuleDetailsWithProgress(courseId, moduleId, userId);

            assertEquals(CompletionStatus.IN_PROGRESS.toString(), dto.moduleInfo().completionStatus());
            assertEquals(1, dto.moduleInfo().completedLessons());
            assertEquals(3, dto.moduleInfo().totalLessons());

            List<LessonProgressDTO> lessons = dto.lessons();
            assertEquals(3, lessons.size());
            assertEquals(CompletionStatus.COMPLETED.toString(), lessons.get(0).status());
            assertEquals(90, lessons.get(0).progress().testResult());
            assertEquals(CompletionStatus.NOT_STARTED.toString(), lessons.get(1).status());
            assertEquals(50, lessons.get(1).progress().testResult());
            assertEquals(CompletionStatus.BLOCKED.toString(), lessons.get(2).status());
        }

        @Test
        @DisplayName("успех — нет прогресса -> NOT_STARTED / BLOCKED")
        void success_notStarted() {
            Course course = course(courseId, "C1");
            Module module = module(moduleId, "M1", course);
            lesson(101L, module);
            lesson(102L, module);

            when(moduleRepository.findByIdWithLessons(moduleId)).thenReturn(Optional.of(module));
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(Set.of(moduleId))).thenReturn(Set.of(101L, 102L));
            when(testAttemptRepository.findPassedLessonIdsByUserIdAndModuleIds(userId, Set.of(moduleId))).thenReturn(Collections.emptySet());
            when(testAttemptRepository.findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(anyLong(), anyLong())).thenReturn(Optional.empty());

            ModuleDetailsDTO dto = service.getModuleDetailsWithProgress(courseId, moduleId, userId);

            assertEquals(CompletionStatus.NOT_STARTED.toString(), dto.moduleInfo().completionStatus());
            assertEquals(0, dto.moduleInfo().completedLessons());

            List<LessonProgressDTO> lessons = dto.lessons();
            assertEquals(2, lessons.size());
            assertEquals(CompletionStatus.NOT_STARTED.toString(), lessons.get(0).status());
            assertEquals(CompletionStatus.BLOCKED.toString(), lessons.get(1).status());
        }
    }

    // Хелперы
    private static Course course(Long id, String title) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        return c;
    }

    private static Module module(Long id, String title, Course course) {
        Module m = new Module();
        m.setId(id);
        m.setTitle(title);
        m.setCourse(course);
        m.setLessons(new ArrayList<>());
        m.getLessons().sort(Comparator.comparing(Lesson::getSequenceOrder));
        return m;
    }

    private static Lesson lesson(Long id, Module module) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setModule(module);
        l.setSequenceOrder(id.intValue());
        module.getLessons().add(l);
        module.getLessons().sort(Comparator.comparing(Lesson::getSequenceOrder));
        return l;
    }
}
