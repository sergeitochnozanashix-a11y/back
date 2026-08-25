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
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.content.properties.CommonProperties;
import software.pxel.learneasy.api.dto.lesson.*;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.LessonMapper;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.repository.LessonRepository;
import software.pxel.learneasy.repository.ModuleRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.service.LessonContentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static software.pxel.learneasy.model.enums.ContentBlockType.TEXT;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonService — операции с уроками")
class LessonServiceImplTest {

    @Mock
    LessonRepository lessonRepository;
    @Mock
    ModuleRepository moduleRepository;
    @Mock
    LessonMapper lessonMapper;
    @Mock
    TestModelRepository testModelRepository;
    @Mock
    LessonContentService lessonContentService;

    @InjectMocks
    LessonServiceImpl service;

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("createLesson(request) — создание")
    class CreateLesson {

        @Test
        @DisplayName("ошибка — moduleId == null")
        void error_nullModuleId() {
            LessonRequest req = new LessonRequest(null, "T", "D", null, List.of());
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createLesson(req));
            assertTrue(ex.getMessage().contains("Module ID is required."));
            verifyNoInteractions(moduleRepository, lessonMapper, lessonRepository);
        }

        @Test
        @DisplayName("ошибка — модуль не найден")
        void error_moduleNotFound() {
            LessonRequest req = new LessonRequest(5L, "T", "D", null, List.of());
            when(moduleRepository.findById(5L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.createLesson(req));
            assertTrue(ex.getMessage().contains("Module not found with ID: 5"));

            verify(moduleRepository).findById(5L);
            verifyNoInteractions(lessonMapper, lessonRepository);
        }

        @Test
        @DisplayName("успех — автонумерация sequenceOrder, когда у нового урока sequence=null")
        void success_autoSequenceWhenNull() {
            LessonRequest req = new LessonRequest(3L, "Title", "Desc", null, List.of());
            Module mod = module(3L, 7);
            when(moduleRepository.findById(3L)).thenReturn(Optional.of(mod));

            Lesson mapped = new Lesson();
            mapped.setTitle("Title");
            mapped.setDescription("Desc");
            mapped.setSequenceOrder(null);
            mapped.setModule(mod);
            when(lessonMapper.toLesson(req)).thenReturn(mapped);

            when(lessonRepository.findMaxSequenceOrderByModuleId(3L)).thenReturn(Optional.of(4)); // → next = 5

            Lesson saved = new Lesson();
            saved.setId(100L);
            saved.setTitle("Title");
            saved.setDescription("Desc");
            saved.setSequenceOrder(5);
            saved.setModule(mod);
            when(lessonRepository.save(any(Lesson.class))).thenReturn(saved);

            LessonWithContentList outDto = LessonWithContentList.builder()
                    .id(100L).title("Title").description("Desc").sequenceOrder(5).moduleSequenceOrder(7)
                    .contentBlocks(List.of()).build();
            when(lessonMapper.toLessonWithContentList(saved, 7, null)).thenReturn(outDto);

            LessonWithContentList result = service.createLesson(req);

            assertEquals(100L, result.id());
            assertEquals(5, result.sequenceOrder());
            assertEquals(7, result.moduleSequenceOrder());

            ArgumentCaptor<Lesson> cap = ArgumentCaptor.forClass(Lesson.class);
            verify(lessonRepository).save(cap.capture());
            assertEquals(5, cap.getValue().getSequenceOrder());
            verify(lessonContentService).setContentOnCreate(cap.getValue(), null, List.of());
        }

        @Test
        @DisplayName("успех — с контентом: вызывается LessonContentService")
        void success_withContentBlocks() {
            CreateBlockBaseDTO child = dtoText("child", false, List.of());
            CreateBlockBaseDTO parent = dtoText("parent", true, List.of(child));
            CreateBlockBaseDTO root2 = dtoText("root2", false, List.of());
            List<CreateBlockBaseDTO> blocks = List.of(parent, root2);

            LessonRequest req = new LessonRequest(9L, "T", "D", null, blocks);
            Module mod = module(9L, 3);
            when(moduleRepository.findById(9L)).thenReturn(Optional.of(mod));

            Lesson mapped = new Lesson();
            mapped.setTitle("T");
            mapped.setDescription("D");
            mapped.setModule(mod);
            when(lessonMapper.toLesson(req)).thenReturn(mapped);
            when(lessonRepository.findMaxSequenceOrderByModuleId(9L)).thenReturn(Optional.of(1)); // next = 2

            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
                Lesson l = inv.getArgument(0);
                l.setId(501L);
                return l;
            });

            LessonWithContentList out = LessonWithContentList.builder()
                    .id(501L).title("T").description("D").sequenceOrder(2).moduleSequenceOrder(3)
                    .contentBlocks(blocks).build();
            when(lessonMapper.toLessonWithContentList(any(Lesson.class), eq(3), isNull())).thenReturn(out);

            LessonWithContentList result = service.createLesson(req);

            assertEquals(501L, result.id());
            ArgumentCaptor<Lesson> cap = ArgumentCaptor.forClass(Lesson.class);
            verify(lessonRepository).save(cap.capture());

            verify(lessonContentService).setContentOnCreate(cap.getValue(), null, blocks);
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("getLessonById(id) — чтение")
    class GetLessonById {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.getLessonById(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            when(lessonRepository.findByIdWithContentBlock(77L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getLessonById(77L));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 77"));
        }

        @Test
        @DisplayName("успех — тест существует, но 0 вопросов → testQuestions=null")
        void success_testExistsButZeroQuestions() {
            Module mod = module(1L, 4);
            Lesson lesson = new Lesson();
            lesson.setId(5L);
            lesson.setModule(mod);
            when(lessonRepository.findByIdWithContentBlock(5L)).thenReturn(Optional.of(lesson));
            when(moduleRepository.findSequenceOrderById(1L)).thenReturn(4);
            when(testModelRepository.existsByLessonId(5L)).thenReturn(true);
            when(testModelRepository.countQuestionsByLessonId(5L)).thenReturn(0);

            LessonWithContentList dto = LessonWithContentList.builder()
                    .id(5L).title("X").description("Y").sequenceOrder(0).moduleSequenceOrder(4).testQuestions(null)
                    .contentBlocks(List.of()).build();
            when(lessonMapper.toLessonWithContentList(lesson, 4, null)).thenReturn(dto);

            LessonWithContentList out = service.getLessonById(5L);

            assertEquals(5L, out.id());
            verify(lessonMapper).toLessonWithContentList(lesson, 4, null);
        }

        @Test
        @DisplayName("успех — тест существует и есть вопросы → testQuestions=cnt")
        void success_testHasQuestions() {
            Module mod = module(2L, 6);
            Lesson lesson = new Lesson();
            lesson.setId(9L);
            lesson.setModule(mod);
            when(lessonRepository.findByIdWithContentBlock(9L)).thenReturn(Optional.of(lesson));
            when(moduleRepository.findSequenceOrderById(2L)).thenReturn(6);
            when(testModelRepository.existsByLessonId(9L)).thenReturn(true);
            when(testModelRepository.countQuestionsByLessonId(9L)).thenReturn(7);

            LessonWithContentList dto = LessonWithContentList.builder()
                    .id(9L).title("A").description("B").sequenceOrder(0).moduleSequenceOrder(6).testQuestions(7)
                    .contentBlocks(List.of()).build();
            when(lessonMapper.toLessonWithContentList(lesson, 6, 7)).thenReturn(dto);

            LessonWithContentList out = service.getLessonById(9L);

            assertEquals(7, out.testQuestions());
            verify(lessonMapper).toLessonWithContentList(lesson, 6, 7);
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("getLessonsByModuleId(...) — фильтрация и пагинация")
    class GetLessonsByModuleId {

        @Test
        @DisplayName("успех — спека передаётся и элементы маппятся")
        void success_filtersAndMaps() {
            Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
            Lesson l1 = new Lesson();
            l1.setId(1L);
            Lesson l2 = new Lesson();
            l2.setId(2L);

            when(lessonRepository.findAllByModuleIdWithFilters(eq(10L), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(l1, l2), pageable, 2));
            when(lessonMapper.toLessonWithoutContentList(l1)).thenReturn(
                    LessonWithoutContentList.builder().id(1L).title("A").description("d").sequenceOrder(0).build());
            when(lessonMapper.toLessonWithoutContentList(l2)).thenReturn(
                    LessonWithoutContentList.builder().id(2L).title("B").description("e").sequenceOrder(1).build());

            Page<LessonWithoutContentList> page = service.getLessonsByModuleId(10L, pageable);

            assertEquals(2, page.getContent().size());
            verify(lessonMapper, times(2)).toLessonWithoutContentList(any(Lesson.class));
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("updateLesson(id, request) — обновление")
    class UpdateLesson {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            UpdateLessonRequest req = new UpdateLessonRequest(1L, "T", "D", 3, null, null);
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.updateLesson(null, req));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            UpdateLessonRequest req = new UpdateLessonRequest(2L, "T", "D", 1, null, null);
            when(lessonRepository.findById(77L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateLesson(77L, req));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 77 for update."));
        }

        @Test
        @DisplayName("успех — обновляет метаданные и вызывает обновление контента")
        void success_updateMetadataAndContent() {
            Module mod = module(2L, 11);
            Lesson existing = new Lesson();
            existing.setId(5L);
            existing.setModule(mod);
            existing.setTitle("Old");
            existing.setDescription("OldD");
            existing.setSequenceOrder(0);

            when(lessonRepository.findById(5L)).thenReturn(Optional.of(existing));

            List<CreateBlockBaseDTO> newBlocks = List.of(
                    dtoText("n1", false, List.of()),
                    dtoText("n2", true, List.of(dtoText("child", false, List.of())))
            );
            UpdateLessonRequest req = new UpdateLessonRequest(2L, "NewT", "NewD", 3, null, newBlocks);

            when(lessonRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
            when(moduleRepository.findSequenceOrderById(2L)).thenReturn(11);
            LessonWithContentList out = LessonWithContentList.builder()
                    .id(5L).title("NewT").description("NewD").sequenceOrder(3).moduleSequenceOrder(11)
                    .contentBlocks(newBlocks).build();
            when(lessonMapper.toLessonWithContentList(existing, 11, null)).thenReturn(out);

            LessonWithContentList result = service.updateLesson(5L, req);

            assertEquals("NewT", result.title());
            assertEquals("NewD", result.description());
            assertEquals(3, result.sequenceOrder());

            verify(lessonContentService).setContentOnUpdate(existing, null, newBlocks);
            verify(lessonRepository).save(existing);
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteLessonById(id) — удаление")
    class DeleteLesson {

        @Test
        @DisplayName("ошибка — id == null")
        void error_nullId() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deleteLessonById(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            when(lessonRepository.existsById(9L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteLessonById(9L));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 9"));
            verify(lessonRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("успех — удалён по id")
        void success_deleted() {
            when(lessonRepository.existsById(3L)).thenReturn(true);

            service.deleteLessonById(3L);

            verify(lessonRepository).deleteById(3L);
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("updateLessonContent(lessonId, blocks) — точечная замена контента (JSON)")
    class UpdateLessonContent {

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            when(lessonRepository.findById(77L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.updateLessonContent(77L, List.of()));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 77"));
        }

        @Test
        @DisplayName("успех — делегирует в сервис и сохраняет")
        void success_buildsHierarchy() {
            Module mod = module(4L, 8);
            Lesson existing = new Lesson();
            existing.setId(12L);
            existing.setModule(mod);

            when(lessonRepository.findById(12L)).thenReturn(Optional.of(existing));
            when(lessonRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
            when(moduleRepository.findSequenceOrderById(4L)).thenReturn(8);

            List<CreateBlockBaseDTO> reqBlocks = List.of(dtoText("p", false, List.of()));

            LessonWithContentList out = LessonWithContentList.builder().id(12L).moduleSequenceOrder(8).build();
            when(lessonMapper.toLessonWithContentList(existing, 8, null)).thenReturn(out);

            LessonWithContentList result = service.updateLessonContent(12L, reqBlocks);

            verify(lessonContentService).updateJsonContent(existing, reqBlocks);
            verify(lessonRepository).save(existing);
            assertEquals(8, result.moduleSequenceOrder());
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("updateLessonMarkdownContent(lessonId, markdown) — точечная замена контента (Markdown)")
    class UpdateLessonMarkdownContent {

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            when(lessonRepository.findById(77L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.updateLessonMarkdownContent(77L, "## New Content"));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 77"));
            verify(lessonContentService, never()).updateMarkdownContent(any(), any());
        }

        @Test
        @DisplayName("успех — делегирует в сервис и сохраняет")
        void success_delegatesToServiceAndSaves() {
            Module mod = module(4L, 8);
            Lesson existing = new Lesson();
            existing.setId(12L);
            existing.setModule(mod);
            String newContent = "### Markdown Header";

            when(lessonRepository.findById(12L)).thenReturn(Optional.of(existing));
            when(lessonRepository.save(existing)).thenReturn(existing);
            when(moduleRepository.findSequenceOrderById(4L)).thenReturn(8);

            LessonWithContentList outDto = LessonWithContentList.builder()
                    .id(12L).content(newContent).moduleSequenceOrder(8).build();
            when(lessonMapper.toLessonWithContentList(existing, 8, null)).thenReturn(outDto);

            LessonWithContentList result = service.updateLessonMarkdownContent(12L, newContent);

            assertEquals(12L, result.id());
            assertEquals(newContent, result.content());
            verify(lessonContentService).updateMarkdownContent(existing, newContent);
            verify(lessonRepository).save(existing);
            verify(lessonMapper).toLessonWithContentList(existing, 8, null);
        }
    }

    // ------------------------------------------------------------------------------------
    @Nested
    @DisplayName("getLessonSequenceOrders(lessonId) — порядковые номера")
    class GetLessonSequence {

        @Test
        @DisplayName("ошибка — null id")
        void error_nullId() {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.getLessonSequenceOrders(null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("ошибка — урок не найден")
        void error_notFound() {
            when(lessonRepository.findById(55L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getLessonSequenceOrders(55L));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 55"));
        }

        @Test
        @DisplayName("успех — возвращает lessonSeq и moduleSeq")
        void success_returnsSeqs() {
            Module mod = module(6L, 13);
            Lesson l = new Lesson();
            l.setId(21L);
            l.setModule(mod);
            l.setSequenceOrder(4);

            when(lessonRepository.findById(21L)).thenReturn(Optional.of(l));
            when(moduleRepository.findSequenceOrderById(6L)).thenReturn(13);

            LessonSequenceDTO s = service.getLessonSequenceOrders(21L);

            assertEquals(4, s.lessonSequenceOrder());
            assertEquals(13, s.moduleSequenceOrder());
        }
    }

    // ------------------------------------------------------------------------------------
    // Хелперы

    private static Module module(Long id, int seqOrder) {
        Module m = new Module();
        m.setId(id);
        m.setSequenceOrder(seqOrder);
        m.setLessons(new ArrayList<>());
        return m;
    }

    /**
     * Примитивная inline-реализация CreateBlockBaseDTO с типом TEXT,
     * чтобы не тянуть реальные классы и не мокать final/record.
     */
    private static CreateBlockBaseDTO dtoText(String content, boolean hasChildren, List<CreateBlockBaseDTO> children) {
        return new CreateBlockBaseDTO() {
            @Override
            public Long id() {
                return null;
            }

            @Override
            public software.pxel.learneasy.model.enums.ContentBlockType blockType() {
                return TEXT;
            }

            @Override
            public CommonProperties properties() {
                return null;
            }

            @Override
            public String content() {
                return content;
            }

            @Override
            public Boolean isHasChildren() {
                return hasChildren;
            }

            @Override
            public List<CreateBlockBaseDTO> children() {
                return children;
            }
        };
    }
}
