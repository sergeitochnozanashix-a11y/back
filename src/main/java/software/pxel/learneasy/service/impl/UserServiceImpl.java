package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.auth.AuthRequest;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.UpdateProfileDTO;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.UserAlreadyExistsException;
import software.pxel.learneasy.mapper.UserMapper;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.model.UserProfile;
import software.pxel.learneasy.repository.UserProfileRepository;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found by username: {}", username);
                    return new UsernameNotFoundException(username);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User login(AuthRequest request) {
        var identifier = request.username();
        log.info("Attempting login or email for user: {}", identifier);

        User user = userRepository.findByUsername(identifier)
                .or(() -> {
                    log.debug("User not found by username '{}', trying by email.", identifier);
                    return userRepository.findByEmail(identifier);
                }).orElseThrow(() -> {
                    log.warn("Login failed: User not found by username or email - {}", identifier);
                    return new JwtAuthenticationException("Wrong username or password. Please, try again!");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed: Invalid password for user - {}", user.getUsername());
            throw new JwtAuthenticationException("Wrong username or password. Please, try again!");
        }

        if (!user.isVerified()) {
            log.warn("Login failed: Email not verified for user - {}", user.getUsername());
            throw new JwtAuthenticationException("Please confirm your email address before logging in.");
        }

        log.info("User {} logged in successfully", user.getUsername());
        return user;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.username());
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed: Username {} already exists", request.username());
            throw new UserAlreadyExistsException("User with username '" + request.username() + "' already exists.");
        }
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: Email {} already exists", request.email());
            throw new UserAlreadyExistsException("User with email '" + request.email() + "' already exists.");
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        log.info("User {} registered successfully with ID {}", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> findAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        // Профили страницы забираем одним запросом: обращение к репозиторию
        // внутри map() дало бы по SELECT на каждую строку списка.
        List<Long> ids = users.getContent().stream().map(User::getId).toList();
        Map<Long, UserProfile> profiles = ids.isEmpty()
                ? Map.of()
                : userProfileRepository.findAllByUserIdIn(ids).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return users.map(user -> userMapper.toDto(user, profiles.get(user.getId())));
    }

    @Transactional(readOnly = true)
    public UserDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user, userProfileRepository.findById(id).orElse(null));
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserDTO updateDto, UserDetails currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!currentUser.getUsername().equals(user.getUsername()) && currentUser.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("You do not have permission to update this user's profile.");
        }

        if (updateDto.username() != null && !updateDto.username().equals(user.getUsername()) && userRepository.existsByUsername(updateDto.username())) {
            throw new ResourceConflictException("Username '" + updateDto.username() + "' is already taken.");
        }

        if (updateDto.email() != null && !updateDto.email().equals(user.getEmail()) && userRepository.existsByEmail(updateDto.email())) {
            throw new ResourceConflictException("Email '" + updateDto.email() + "' is already taken.");
        }

        userMapper.updateUserFromDto(updateDto, user);
        userRepository.save(user);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUserProfile(User currentUser) {
        User user = reloadCurrentUser(currentUser);
        return userMapper.toDto(user, userProfileRepository.findById(user.getId()).orElse(null));
    }

    @Override
    @Transactional
    public UserDTO updateCurrentUserProfile(User currentUser, UpdateProfileDTO updateDTO) {
        User user = reloadCurrentUser(currentUser);

        if (updateDTO.email() != null && !updateDTO.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDTO.email())) {
                throw new ResourceConflictException("Email '" + updateDTO.email() + "' is already taken.");
            }
            user.setEmail(updateDTO.email());
            userRepository.save(user);
        }

        // Строки профиля может ещё не быть: она создаётся при первом сохранении,
        // а не вместе с пользователем при регистрации.
        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseGet(() -> new UserProfile(user));

        userMapper.updateProfileFromDto(updateDTO, profile);
        UserProfile saved = userProfileRepository.save(profile);

        log.info("Profile updated for user {}", user.getUsername());
        return userMapper.toDto(user, saved);
    }

    /**
     * Principal кладётся в SecurityContext при разборе JWT и к моменту вызова
     * уже отсоединён от сессии Hibernate. Перечитываем его, чтобы работать
     * с управляемой сущностью и со свежими данными.
     */
    private User reloadCurrentUser(User currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + currentUser.getId()));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
