package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import software.pxel.learneasy.api.dto.auth.AuthRequest;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.model.User;

public interface UserService {
    User findByUsername(String username);

    User login(AuthRequest authRequest);

    User register(RegisterRequest registerRequest);

    UserDTO updateUser(Long id, UpdateUserDTO updateDTO, UserDetails currentUser);

    UserDTO findUserById(Long id);

    Page<UserDTO> findAllUsers(Pageable pageable);

    void deleteUser(Long id);
}
