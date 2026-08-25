package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.pxel.learneasy.api.dto.auth.AuthRequest;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.UserAlreadyExistsException;
import software.pxel.learneasy.mapper.UserMapper;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — операции над пользователями")
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserServiceImpl userService;

    private User testUser;
    private AuthRequest authLoginRequest;
    private AuthRequest authEmailRequest;
    private RegisterRequest registerRequest;

    private String rawPassword;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        rawPassword = "plainPassword";
        encodedPassword = "encodedPassword123";

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(encodedPassword);
        testUser.setVerified(true);

        authLoginRequest = new AuthRequest("testuser", rawPassword);
        authEmailRequest = new AuthRequest("test@example.com", rawPassword);
        registerRequest = new RegisterRequest("newuser", "new@example.com", rawPassword);
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("findByUsername(username) — поиск по логину")
    class FindByUsername {

        @Test
        @DisplayName("успех — возвращает пользователя")
        void success_returnsUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            User found = userService.findByUsername("testuser");

            assertNotNull(found);
            assertEquals("testuser", found.getUsername());
            verify(userRepository).findByUsername("testuser");
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — бросает UsernameNotFoundException")
        void error_throwsUsernameNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            UsernameNotFoundException ex = assertThrows(
                    UsernameNotFoundException.class,
                    () -> userService.findByUsername("ghost")
            );

            assertEquals("ghost", ex.getMessage());
            verify(userRepository).findByUsername("ghost");
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("login(request) — аутентификация")
    class Login {

        @Test
        @DisplayName("успех — корректный пароль")
        void success_validPassword() {
            when(userRepository.findByUsername(authLoginRequest.username())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(authEmailRequest.password(), testUser.getPassword())).thenReturn(true);

            User loggedIn = userService.login(authLoginRequest);

            assertNotNull(loggedIn);
            assertEquals("testuser", loggedIn.getUsername());

            verify(userRepository).findByUsername(authLoginRequest.username());
            verify(userRepository, never()).findByEmail(anyString());
            verify(passwordEncoder).matches(authLoginRequest.password(), testUser.getPassword());
        }

        @Test
        @DisplayName("успех — вход по email")
        void success_whenLoginByEmail() {
            when(userRepository.findByUsername(authEmailRequest.username())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(authEmailRequest.username())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(authEmailRequest.password(), testUser.getPassword())).thenReturn(true);

            User loggedInUser = userService.login(authEmailRequest);

            assertNotNull(loggedInUser);
            assertEquals("testuser", loggedInUser.getUsername());

            verify(userRepository).findByUsername(authEmailRequest.username());
            verify(userRepository).findByEmail(authEmailRequest.username());
            verify(passwordEncoder).matches(authEmailRequest.password(), testUser.getPassword());
        }

        @Test
        @DisplayName("ошибка — пользователь не найден")
        void error_userNotFound() {
            when(userRepository.findByUsername(authLoginRequest.username())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(authLoginRequest.username())).thenReturn(Optional.empty());

            JwtAuthenticationException ex = assertThrows(
                    JwtAuthenticationException.class,
                    () -> userService.login(authLoginRequest)
            );

            assertEquals("Wrong username or password. Please, try again!", ex.getMessage());
            verify(userRepository).findByUsername(authLoginRequest.username());
            verify(userRepository).findByEmail(authLoginRequest.username());
            verifyNoInteractions(passwordEncoder);
            verifyNoMoreInteractions(userRepository, userMapper);
        }

        @Test
        @DisplayName("ошибка — неверный пароль (при входе по логину)")
        void error_wrongPassword() {
            when(userRepository.findByUsername(authLoginRequest.username())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(eq(authLoginRequest.password()), any())).thenReturn(false);

            JwtAuthenticationException ex = assertThrows(
                    JwtAuthenticationException.class,
                    () -> userService.login(authLoginRequest)
            );

            assertEquals("Wrong username or password. Please, try again!", ex.getMessage());
            verify(userRepository).findByUsername(authLoginRequest.username());
            verify(passwordEncoder).matches(eq(authLoginRequest.password()), any());
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — неверный пароль (при входе по email)")
        void error_whenWrongPasswordForLoginByEmail() {
            when(userRepository.findByUsername(authEmailRequest.username())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(authEmailRequest.username())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(authEmailRequest.password(), testUser.getPassword())).thenReturn(false);

            assertThrows(
                    JwtAuthenticationException.class,
                    () -> userService.login(authEmailRequest)
            );

            verify(userRepository).findByUsername(authEmailRequest.username());
            verify(userRepository).findByEmail(authEmailRequest.username());
            verify(passwordEncoder).matches(authEmailRequest.password(), testUser.getPassword());
        }

        @Test
        @DisplayName("ошибка — пользователь не найден ни по логину, ни по email")
        void error_whenUserNotFoundByBoth() {
            AuthRequest nonExistentRequest = new AuthRequest("nonexistent", "password123");
            when(userRepository.findByUsername(nonExistentRequest.username())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(nonExistentRequest.username())).thenReturn(Optional.empty());

            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> userService.login(nonExistentRequest)
            );

            assertEquals("Wrong username or password. Please, try again!", exception.getMessage());

            verify(userRepository).findByUsername(nonExistentRequest.username());
            verify(userRepository).findByEmail(nonExistentRequest.username());
            verifyNoInteractions(passwordEncoder);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("register(request) — регистрация")
    class Register {

        @Test
        @DisplayName("успех — кодирует пароль и сохраняет пользователя")
        void success_registersUser() {
            when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);

            User mapped = copyUser(null, "newuser", "new@example.com");
            when(userMapper.toUser(registerRequest)).thenReturn(mapped);
            when(passwordEncoder.encode(registerRequest.password())).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0, User.class);
                u.setId(1L);
                return u;
            });

            User registered = userService.register(registerRequest);

            assertNotNull(registered);
            assertEquals("newuser", registered.getUsername());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User toSave = captor.getValue();
            assertEquals(encodedPassword, toSave.getPasswordHash());

            verify(userRepository).existsByUsername(registerRequest.username());
            verify(userRepository).existsByEmail(registerRequest.email());
            verify(userMapper).toUser(registerRequest);
            verify(passwordEncoder).encode(registerRequest.password());
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — такой username уже существует")
        void error_usernameTaken() {
            when(userRepository.existsByUsername(registerRequest.username())).thenReturn(true);

            UserAlreadyExistsException ex = assertThrows(
                    UserAlreadyExistsException.class,
                    () -> userService.register(registerRequest)
            );

            assertEquals("User with username '" + registerRequest.username() + "' already exists.", ex.getMessage());
            verify(userRepository).existsByUsername(registerRequest.username());
            verify(userRepository, never()).existsByEmail(anyString());
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("ошибка — такой email уже существует")
        void error_emailTaken() {
            when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

            UserAlreadyExistsException ex = assertThrows(
                    UserAlreadyExistsException.class,
                    () -> userService.register(registerRequest)
            );

            assertEquals("User with email '" + registerRequest.email() + "' already exists.", ex.getMessage());
            verify(userRepository).existsByUsername(registerRequest.username());
            verify(userRepository).existsByEmail(registerRequest.email());
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("findUserById(id) — поиск по идентификатору")
    class FindUserById {

        @Test
        @DisplayName("успех — маппит в DTO")
        void success_mapsToDto() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            UserDTO dto = mock(UserDTO.class, RETURNS_DEEP_STUBS);
            when(userMapper.toDto(testUser)).thenReturn(dto);

            UserDTO result = userService.findUserById(1L);

            assertNotNull(result);
            verify(userRepository).findById(1L);
            verify(userMapper).toDto(testUser);
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — пользователь не найден")
        void error_notFound() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.findUserById(42L)
            );

            assertTrue(ex.getMessage().contains("User not found with id: 42"));
            verify(userRepository).findById(42L);
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("findAllUsers(pageable) — постраничный список")
    class FindAllUsers {

        @Test
        @DisplayName("успех — маппит каждую сущность в DTO")
        void success_mapsEachEntity() {
            PageRequest pageable = PageRequest.of(0, 2);
            List<User> users = List.of(
                    copyUser(1L, "u1", "u1@mail.com"),
                    copyUser(2L, "u2", "u2@mail.com")
            );
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(users));
            when(userMapper.toDto(any(User.class))).thenReturn(mock(UserDTO.class));

            Page<UserDTO> page = userService.findAllUsers(pageable);

            assertNotNull(page);
            assertEquals(2, page.getContent().size());
            verify(userRepository).findAll(pageable);
            verify(userMapper, times(2)).toDto(any(User.class));
            verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("updateUser(id, dto, currentUser) — обновление профиля")
    class UpdateUser {

        @Test
        @DisplayName("ошибка — пользователь не найден")
        void error_targetUserNotFound() {
            when(userRepository.findById(13L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.updateUser(13L, null, mock(UserDetails.class))
            );

            assertTrue(ex.getMessage().contains("User not found with id: 13"));
            verify(userRepository).findById(13L);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — нет прав (не владелец и не ADMIN)")
        void error_accessDeniedForForeignUser() {
            User target = copyUser(5L, "victim", "v@mail.com");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));

            UserDetails stranger = mockPrincipalWithRoles("other", "USER");

            AccessDeniedException ex = assertThrows(
                    AccessDeniedException.class,
                    () -> userService.updateUser(5L, null, stranger)
            );

            assertTrue(ex.getMessage().contains("You do not have permission"));
            verify(userRepository).findById(5L);
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("ошибка — конфликт username")
        void error_usernameConflict() {
            User target = copyUser(7L, "old", "old@mail.com");
            when(userRepository.findById(7L)).thenReturn(Optional.of(target));

            UserDetails owner = mockOwner("old");

            UpdateUserDTO dto = mock(UpdateUserDTO.class);
            when(dto.username()).thenReturn("newName");

            when(userRepository.existsByUsername("newName")).thenReturn(true);

            ResourceConflictException ex = assertThrows(
                    ResourceConflictException.class,
                    () -> userService.updateUser(7L, dto, owner)
            );

            assertTrue(ex.getMessage().contains("Username 'newName' is already taken."));
            verify(userRepository).findById(7L);
            verify(userRepository).existsByUsername("newName");
            verify(userRepository, never()).existsByEmail(anyString());
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("ошибка — конфликт email")
        void error_emailConflict() {
            User target = copyUser(8L, "old", "old@mail.com");
            when(userRepository.findById(8L)).thenReturn(Optional.of(target));

            UserDetails owner = mockOwner("old");

            UpdateUserDTO dto = mock(UpdateUserDTO.class);
            when(dto.username()).thenReturn(null);
            when(dto.email()).thenReturn("new@mail.com");

            when(userRepository.existsByEmail("new@mail.com")).thenReturn(true);

            ResourceConflictException ex = assertThrows(
                    ResourceConflictException.class,
                    () -> userService.updateUser(8L, dto, owner)
            );

            assertTrue(ex.getMessage().contains("Email 'new@mail.com' is already taken."));
            verify(userRepository).findById(8L);
            verify(userRepository).existsByEmail("new@mail.com");
            verify(userRepository, never()).existsByUsername(anyString());
            verifyNoInteractions(userMapper, passwordEncoder);
            verifyNoMoreInteractions(userRepository);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("deleteUser(id) — удаление пользователя")
    class DeleteUser {

        @Test
        @DisplayName("успех — удаляет при наличии")
        void success_deletesWhenExists() {
            when(userRepository.existsById(3L)).thenReturn(true);

            userService.deleteUser(3L);

            verify(userRepository).existsById(3L);
            verify(userRepository).deleteById(3L);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(userMapper, passwordEncoder);
        }

        @Test
        @DisplayName("ошибка — не найден")
        void error_notFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.deleteUser(99L)
            );

            assertTrue(ex.getMessage().contains("User not found with id: 99"));
            verify(userRepository).existsById(99L);
            verify(userRepository, never()).deleteById(anyLong());
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(userMapper, passwordEncoder);
        }
    }

    // ------------------------------------------------------------
    // Хелперы
    private static UserDetails mockOwner(String username) {
        UserDetails p = mock(UserDetails.class);
        when(p.getUsername()).thenReturn(username);
        return p;
    }

    private static UserDetails mockPrincipalWithRoles(String username, String... roles) {
        UserDetails p = mock(UserDetails.class);
        when(p.getUsername()).thenReturn(username);
        java.util.Set<org.springframework.security.core.GrantedAuthority> auths =
                java.util.Arrays.stream(roles)
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        org.mockito.Mockito.doReturn(auths).when(p).getAuthorities();
        return p;
    }

    private static User copyUser(Long id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        return u;
    }
}
