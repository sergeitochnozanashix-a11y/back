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
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.exception.JwtAuthenticationException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.UserAlreadyExistsException;
import software.pxel.learneasy.mapper.UserMapper;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
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
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
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

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
